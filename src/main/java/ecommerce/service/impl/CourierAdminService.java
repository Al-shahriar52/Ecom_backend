package ecommerce.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import ecommerce.courier.CourierProvider;
import ecommerce.courier.CourierRegistry;
import ecommerce.courier.pathao.PathaoApiException;
import ecommerce.courier.pathao.PathaoClient;
import ecommerce.courier.pathao.PathaoProvider;
import ecommerce.dto.courier.*;
import ecommerce.entity.CourierAccount;
import ecommerce.entity.CourierStore;
import ecommerce.exceptionHandling.BadRequestException;
import ecommerce.repository.CourierAccountRepository;
import ecommerce.repository.CourierStoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** Admin-side courier settings: provider switches, Pathao stores, location lookups and fee quotes. */
@Service
@RequiredArgsConstructor
public class CourierAdminService {

    private final CourierRegistry registry;
    private final CourierAccountRepository accounts;
    private final CourierStoreRepository stores;
    private final PathaoClient pathao;

    // ------------------------------------------------------------ providers

    public List<ProviderInfoDto> listProviders() {
        List<ProviderInfoDto> out = new ArrayList<>();
        for (CourierProvider p : registry.all()) {
            Optional<CourierAccount> acc = accounts.findByProvider(p.key());
            out.add(new ProviderInfoDto(p.key(), p.displayName(),
                    acc.map(CourierAccount::isEnabled).orElse(false),
                    acc.map(CourierAccount::isDefaultProvider).orElse(false),
                    p.isConfigured(), p.isSandbox(), p.capabilities()));
        }
        out.sort(Comparator.comparing(ProviderInfoDto::getDisplayName));
        return out;
    }

    @Transactional
    public List<ProviderInfoDto> updateProvider(String key, UpdateProviderRequest req) {
        CourierProvider provider = registry.all().stream().filter(p -> p.key().equals(key)).findFirst()
                .orElseThrow(() -> new BadRequestException("Unknown courier: " + key));
        CourierAccount acc = accounts.findByProvider(key).orElseGet(() -> {
            CourierAccount a = new CourierAccount();
            a.setProvider(key);
            a.setDisplayName(provider.displayName());
            return a;
        });

        if (req.getEnabled() != null) {
            if (req.getEnabled() && !provider.isConfigured()) {
                throw new BadRequestException(provider.displayName() + " is not configured on the server yet (missing credentials)");
            }
            acc.setEnabled(req.getEnabled());
            if (!req.getEnabled()) acc.setDefaultProvider(false);
        }
        if (Boolean.TRUE.equals(req.getDefaultProvider())) {
            if (!acc.isEnabled()) throw new BadRequestException("Enable " + provider.displayName() + " before making it the default");
            for (CourierAccount other : accounts.findAll()) {
                if (other.isDefaultProvider() && !other.getProvider().equals(key)) {
                    other.setDefaultProvider(false);
                    accounts.save(other);
                }
            }
            acc.setDefaultProvider(true);
        } else if (Boolean.FALSE.equals(req.getDefaultProvider())) {
            acc.setDefaultProvider(false);
        }
        accounts.save(acc);

        // Never leave the shop without a default courier.
        if (accounts.findFirstByEnabledTrueAndDefaultProviderTrue().isEmpty()) {
            accounts.findAll().stream().filter(CourierAccount::isEnabled).findFirst().ifPresent(a -> {
                a.setDefaultProvider(true);
                accounts.save(a);
            });
        }
        return listProviders();
    }

    public void testPathao() {
        try {
            pathao.testConnection();
        } catch (PathaoApiException e) {
            throw new BadRequestException("Pathao connection failed: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------ Pathao stores

    public List<CourierStore> listPathaoStores() {
        return stores.findByProviderOrderByDefaultStoreDescNameAsc(PathaoProvider.KEY);
    }

    /** Pulls the store list from Pathao into courier_stores. Keeps our own default choice. */
    @Transactional
    public List<CourierStore> syncPathaoStores() {
        JsonNode remote;
        try {
            remote = pathao.listStores();
        } catch (PathaoApiException e) {
            throw new BadRequestException("Could not load Pathao stores: " + e.getMessage());
        }
        boolean hadDefault = stores.findByProviderOrderByDefaultStoreDescNameAsc(PathaoProvider.KEY)
                .stream().anyMatch(CourierStore::isDefaultStore);
        Long pathaoDefaultId = null;

        for (JsonNode n : remote) {
            long ext = n.path("store_id").asLong();
            CourierStore s = stores.findByProviderAndExternalStoreId(PathaoProvider.KEY, ext).orElseGet(() -> {
                CourierStore c = new CourierStore();
                c.setProvider(PathaoProvider.KEY);
                c.setExternalStoreId(ext);
                return c;
            });
            s.setName(n.path("store_name").asText(null));
            s.setAddress(n.path("store_address").asText(null));
            s.setActive(n.path("is_active").asInt(1) == 1);
            s.setCityId(n.hasNonNull("city_id") ? n.get("city_id").asLong() : null);
            s.setZoneId(n.hasNonNull("zone_id") ? n.get("zone_id").asLong() : null);
            s.setHubId(n.hasNonNull("hub_id") ? n.get("hub_id").asLong() : null);
            s.setSyncedAt(java.time.LocalDateTime.now());
            stores.save(s);
            if (n.path("is_default_store").asBoolean(false)) pathaoDefaultId = ext;
        }

        List<CourierStore> all = stores.findByProviderOrderByDefaultStoreDescNameAsc(PathaoProvider.KEY);
        if (!hadDefault && !all.isEmpty()) {
            final Long wanted = pathaoDefaultId;
            CourierStore pick = all.stream().filter(s -> wanted != null && wanted.equals(s.getExternalStoreId()))
                    .findFirst().orElse(all.get(0));
            pick.setDefaultStore(true);
            stores.save(pick);
        }
        return stores.findByProviderOrderByDefaultStoreDescNameAsc(PathaoProvider.KEY);
    }

    @Transactional
    public List<CourierStore> createPathaoStore(CreatePathaoStoreRequest r) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", r.getName());
        payload.put("contact_name", r.getContactName());
        payload.put("contact_number", r.getContactNumber());
        if (r.getSecondaryContact() != null && !r.getSecondaryContact().isBlank()) payload.put("secondary_contact", r.getSecondaryContact());
        if (r.getOtpNumber() != null && !r.getOtpNumber().isBlank()) payload.put("otp_number", r.getOtpNumber());
        payload.put("address", r.getAddress());
        payload.put("city_id", r.getCityId());
        payload.put("zone_id", r.getZoneId());
        payload.put("area_id", r.getAreaId());
        try {
            pathao.createStore(payload);
        } catch (PathaoApiException e) {
            throw new BadRequestException("Pathao rejected the store: " + e.getMessage());
        }
        // Pathao approves new stores in about an hour, so it may not be listed yet - sync is best effort.
        try { return syncPathaoStores(); } catch (BadRequestException e) { return listPathaoStores(); }
    }

    @Transactional
    public List<CourierStore> setDefaultPathaoStore(Long id) {
        CourierStore target = stores.findById(id).filter(s -> PathaoProvider.KEY.equals(s.getProvider()))
                .orElseThrow(() -> new BadRequestException("Store not found"));
        if (!target.isActive()) throw new BadRequestException("This store is not active yet (Pathao is still approving it)");
        for (CourierStore s : stores.findByProviderOrderByDefaultStoreDescNameAsc(PathaoProvider.KEY)) {
            boolean should = s.getId().equals(id);
            if (s.isDefaultStore() != should) { s.setDefaultStore(should); stores.save(s); }
        }
        return stores.findByProviderOrderByDefaultStoreDescNameAsc(PathaoProvider.KEY);
    }

    // ------------------------------------------------------------ Pathao locations + quote

    public JsonNode pathaoCities() { return wrap(() -> pathao.listCities()); }
    public JsonNode pathaoZones(long cityId) { return wrap(() -> pathao.listZones(cityId)); }
    public JsonNode pathaoAreas(long zoneId) { return wrap(() -> pathao.listAreas(zoneId)); }

    public JsonNode pathaoQuote(PathaoQuoteRequest r) {
        Long storeId = r.getStoreId();
        if (storeId == null) {
            storeId = stores.findByProviderAndActiveTrueOrderByDefaultStoreDescIdAsc(PathaoProvider.KEY).stream()
                    .findFirst().map(CourierStore::getExternalStoreId)
                    .orElseThrow(() -> new BadRequestException("No active Pathao store. Sync or create one first."));
        }
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("store_id", storeId);
        p.put("item_type", 2);
        p.put("delivery_type", r.getDeliveryType());
        p.put("item_weight", r.getWeight());
        p.put("recipient_city", r.getCityId());
        p.put("recipient_zone", r.getZoneId());
        return wrap(() -> pathao.priceQuote(p));
    }

    private JsonNode wrap(java.util.function.Supplier<JsonNode> call) {
        try {
            return call.get();
        } catch (PathaoApiException e) {
            throw new BadRequestException("Pathao: " + e.getMessage());
        }
    }
}

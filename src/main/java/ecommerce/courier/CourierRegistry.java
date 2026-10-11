package ecommerce.courier;

import ecommerce.entity.CourierAccount;
import ecommerce.exceptionHandling.BadRequestException;
import ecommerce.repository.CourierAccountRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Finds the adapter for a courier key, honouring courier_account (enabled / default). */
@Component
public class CourierRegistry {

    private static final String FALLBACK_KEY = "steadfast";

    private final Map<String, CourierProvider> providers;
    private final CourierAccountRepository accounts;

    public CourierRegistry(List<CourierProvider> providers, CourierAccountRepository accounts) {
        this.providers = providers.stream().collect(Collectors.toMap(CourierProvider::key, Function.identity()));
        this.accounts = accounts;
    }

    /** Provider for an explicit key, or the default courier when key is null/blank. */
    public CourierProvider resolve(String key) {
        String k = (key == null || key.isBlank()) ? defaultKey() : key.trim().toLowerCase();
        CourierProvider provider = providers.get(k);
        if (provider == null) throw new BadRequestException("Unknown courier: " + k);
        if (!provider.isConfigured()) throw new BadRequestException(provider.displayName() + " is not configured on the server");
        // A courier with no account row yet is treated as enabled only for the built-in fallback.
        boolean enabled = accounts.findByProvider(k).map(CourierAccount::isEnabled).orElse(k.equals(FALLBACK_KEY));
        if (!enabled) throw new BadRequestException(provider.displayName() + " is disabled");
        return provider;
    }

    public CourierProvider getDefault() { return resolve(null); }

    public List<CourierProvider> all() { return List.copyOf(providers.values()); }

    private String defaultKey() {
        return accounts.findFirstByEnabledTrueAndDefaultProviderTrue()
                .map(CourierAccount::getProvider)
                .orElse(FALLBACK_KEY);
    }
}

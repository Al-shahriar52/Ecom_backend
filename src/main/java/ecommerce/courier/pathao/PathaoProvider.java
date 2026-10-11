package ecommerce.courier.pathao;

import com.fasterxml.jackson.databind.JsonNode;
import ecommerce.courier.*;
import ecommerce.entity.CourierStore;
import ecommerce.entity.Invoice;
import ecommerce.entity.Order;
import ecommerce.entity.OrderItem;
import ecommerce.repository.CourierStoreRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Pathao adapter. Pickup is automatic from the store set on each order, so there is no pickup request.
 * Parcels are booked one by one (not via the async bulk endpoint) because bulk returns no consignment ids.
 */
@Component
public class PathaoProvider implements CourierProvider {

    public static final String KEY = "pathao";

    private final PathaoClient client;
    private final CourierStoreRepository stores;

    @Value("${pathao.default-weight:0.5}")
    private double defaultWeight;

    public PathaoProvider(PathaoClient client, CourierStoreRepository stores) {
        this.client = client;
        this.stores = stores;
    }

    @Override public String key() { return KEY; }
    @Override public String displayName() { return "Pathao"; }
    @Override public boolean isConfigured() { return client.isConfigured(); }
    @Override public boolean isSandbox() { return client.isSandbox(); }

    @Override
    public CourierCapabilities capabilities() {
        return new CourierCapabilities(true, false, false, false, true, true);
    }

    @Override
    public List<BookingResult> book(List<BookingRequest> parcels) {
        List<BookingResult> results = new ArrayList<>();
        if (parcels == null || parcels.isEmpty()) return results;

        Optional<CourierStore> store = stores.findByProviderAndActiveTrueOrderByDefaultStoreDescIdAsc(KEY).stream().findFirst();
        if (store.isEmpty()) {
            String msg = "No active Pathao store. Create or sync a store in Courier settings first.";
            parcels.forEach(p -> results.add(BookingResult.failed(p.orderId(), msg)));
            return results;
        }

        for (BookingRequest p : parcels) {
            try {
                Map<String, Object> payload = buildPayload(p, store.get().getExternalStoreId());
                JsonNode data = client.createOrder(payload);
                String cid = data.path("consignment_id").asText(null);
                if (cid == null || cid.isBlank()) {
                    results.add(BookingResult.failed(p.orderId(), "Pathao accepted the order but returned no consignment id"));
                    continue;
                }
                Double fee = data.hasNonNull("delivery_fee") ? data.get("delivery_fee").asDouble() : null;
                results.add(BookingResult.ok(p.orderId(), cid, null, fee));
            } catch (IllegalArgumentException e) {
                results.add(BookingResult.failed(p.orderId(), e.getMessage())); // our own pre-check
            } catch (PathaoApiException e) {
                results.add(BookingResult.failed(p.orderId(), e.getMessage()));
            }
        }
        return results;
    }

    /** Builds the /orders payload and enforces Pathao's field rules before spending an API call. */
    Map<String, Object> buildPayload(BookingRequest p, Long storeId) {
        Order o = p.order();
        Invoice inv = p.invoice();
        String inv_no = inv.getInvoiceNumber();

        String name = o.getName() == null ? "" : o.getName().trim();
        if (name.length() < 3 || name.length() > 100) {
            throw new IllegalArgumentException(inv_no + ": recipient name must be 3-100 characters");
        }

        String phone = normalizePhone(o.getPhoneNumber());
        if (phone == null) {
            throw new IllegalArgumentException(inv_no + ": phone must be 11 digits starting with 01");
        }

        String address = joinAddress(o.getShippingAddress(), o.getArea(), o.getCity());
        if (address.length() < 10) {
            throw new IllegalArgumentException(inv_no + ": address must be at least 10 characters");
        }
        if (address.length() > 220) address = address.substring(0, 220);

        int qty = 0;
        StringBuilder desc = new StringBuilder();
        if (o.getOrderItems() != null) {
            for (OrderItem it : o.getOrderItems()) {
                qty += it.getQuantity();
                if (it.getProduct() != null && it.getProduct().getName() != null) {
                    if (desc.length() > 0) desc.append(", ");
                    desc.append(it.getProduct().getName()).append(" x").append(it.getQuantity());
                }
            }
        }
        if (qty < 1) qty = 1;

        long cod = o.getPaymentMethod() == ecommerce.enums.PaymentMethod.COD && inv.getTotalAmount() != null
                ? Math.round(inv.getTotalAmount()) : 0L;

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("store_id", storeId);
        m.put("merchant_order_id", inv_no);
        m.put("recipient_name", name);
        m.put("recipient_phone", phone);
        m.put("recipient_address", address);
        m.put("delivery_type", 48);   // normal delivery
        m.put("item_type", 2);        // parcel
        if (o.getOrderNote() != null && !o.getOrderNote().isBlank()) m.put("special_instruction", o.getOrderNote());
        m.put("item_quantity", qty);
        m.put("item_weight", Math.min(10.0, Math.max(0.5, defaultWeight)));
        if (desc.length() > 0) m.put("item_description", desc.length() > 250 ? desc.substring(0, 250) : desc.toString());
        m.put("amount_to_collect", cod);
        return m;
    }

    /** "+8801712-345678" / "8801712345678" / "01712 345 678" -> "01712345678", or null if not a valid BD mobile. */
    static String normalizePhone(String raw) {
        if (raw == null) return null;
        String d = raw.replaceAll("\\D", "");
        if (d.startsWith("880")) d = d.substring(2);
        if (d.length() == 10 && d.startsWith("1")) d = "0" + d;
        return d.matches("^01\\d{9}$") ? d : null;
    }

    private static String joinAddress(String... parts) {
        StringJoiner j = new StringJoiner(", ");
        for (String s : parts) if (s != null && !s.isBlank()) j.add(s.trim());
        return j.toString();
    }

    @Override
    public Optional<CourierStatus> fetchStatus(String consignmentId, String invoiceNumber) {
        JsonNode info = client.orderInfo(consignmentId);
        String raw = info.hasNonNull("order_status_slug") ? info.get("order_status_slug").asText()
                : info.hasNonNull("order_status") ? info.get("order_status").asText() : null;
        if (raw == null) return Optional.empty();
        return Optional.of(new CourierStatus(mapStatus(raw), raw, null));
    }

    /**
     * Pathao status text -> DeliveryStatus. Their doc only shows "Pending", so this matches on keywords
     * of Pathao's usual statuses; anything unrecognised returns null (no change, raw text is still saved).
     */
    static ecommerce.enums.DeliveryStatus mapStatus(String raw) {
        String s = raw.toLowerCase().replaceAll("[_\\-]+", " ").trim();
        if (s.contains("partial")) return ecommerce.enums.DeliveryStatus.DELIVERED;
        if (s.equals("delivered") || s.startsWith("delivered")) return ecommerce.enums.DeliveryStatus.DELIVERED;
        if (s.contains("cancel")) return ecommerce.enums.DeliveryStatus.CANCELLED;
        if (s.contains("return")) return ecommerce.enums.DeliveryStatus.RETURNED;
        if (s.contains("picked up") || s.contains("transit") || s.contains("hub")
                || s.contains("out for delivery") || s.contains("assigned for delivery")) {
            return ecommerce.enums.DeliveryStatus.IN_TRANSIT;
        }
        if (s.contains("pickup")) return ecommerce.enums.DeliveryStatus.READY_FOR_PICKUP;
        if (s.contains("pending")) return ecommerce.enums.DeliveryStatus.PENDING;
        return null;
    }
}

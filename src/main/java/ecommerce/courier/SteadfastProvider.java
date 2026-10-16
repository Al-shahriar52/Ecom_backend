package ecommerce.courier;

import com.fasterxml.jackson.databind.JsonNode;
import ecommerce.entity.Invoice;
import ecommerce.entity.Order;
import ecommerce.service.impl.SteadfastService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import ecommerce.enums.DeliveryStatus;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Steadfast adapter. Wraps the existing SteadfastService HTTP client and owns the response parsing
 * that used to live in AdminOrderServiceImpl. Behaviour is unchanged:
 * one parcel -> single API; several -> bulk API, falling back to one-by-one if bulk fails.
 */
@Component
@RequiredArgsConstructor
public class SteadfastProvider implements CourierProvider {

    private final SteadfastService client;

    /**
     * Steadfast has no "picked up" status. When true, a parcel still pending/in_review/hold is treated as
     * picked up once its tracking history has a step beyond "Consignment created" (tracking_type > 1).
     * That rule is inferred from the doc's example, so it can be switched off.
     */
    @Value("${courier.sync.steadfast-pickup-detection:true}")
    private boolean pickupDetection;

    @Override public String key() { return "steadfast"; }
    @Override public String displayName() { return "Steadfast"; }

    @Override
    public CourierCapabilities capabilities() {
        return new CourierCapabilities(true, true, true, true, false, true);
    }

    @Override
    public List<BookingResult> book(List<BookingRequest> parcels) {
        List<BookingResult> results = new ArrayList<>();
        if (parcels == null || parcels.isEmpty()) return results;

        if (parcels.size() == 1) {
            // Single order: errors propagate to the caller, as before.
            BookingRequest p = parcels.get(0);
            JsonNode response = client.sendSinglePickupRequest(p.order(), p.invoice(), p.codOverride());
            results.addAll(parse(response, parcels));
            return results;
        }

        try {
            List<Order> orders = parcels.stream().map(BookingRequest::order).toList();
            Map<Long, Invoice> invoiceMap = new HashMap<>();
            parcels.forEach(p -> invoiceMap.put(p.orderId(), p.invoice()));

            Map<Long, Double> overrides = new HashMap<>();
            parcels.forEach(p -> { if (p.codOverride() != null) overrides.put(p.orderId(), p.codOverride()); });

            JsonNode bulk = client.sendBulkPickupRequest(orders, invoiceMap, overrides);
            if (bulk == null || (bulk.has("status") && bulk.get("status").asInt() >= 400)) {
                throw new RuntimeException("Bulk API returned an error or null response.");
            }
            results.addAll(parse(bulk, parcels));
        } catch (Exception e) {
            System.err.println("Bulk API failed. Falling back to single API loop. Error: " + e.getMessage());
            results.clear();
            for (BookingRequest p : parcels) {
                try {
                    JsonNode single = client.sendSinglePickupRequest(p.order(), p.invoice(), p.codOverride());
                    results.addAll(parse(single, List.of(p)));
                } catch (Exception ex) {
                    System.err.println("Fallback failed for invoice " + p.invoice().getInvoiceNumber() + ": " + ex.getMessage());
                    results.add(BookingResult.failed(p.orderId(), ex.getMessage()));
                }
            }
        }
        return results;
    }

    @Override
    public PickupOutcome requestPickup(PickupSpec s) {
        return client.createPickupRequest(s.addressId(), s.policeStationId(), s.address(),
                s.contactNumber(), s.note(), s.estimQty());
    }

    /** Parses single ({consignment:{...}}) and bulk ([...] or {data:[...]}) responses. */
    private List<BookingResult> parse(JsonNode response, List<BookingRequest> sent) {
        List<BookingResult> out = new ArrayList<>();
        if (response == null) return out;

        List<JsonNode> items = new ArrayList<>();
        if (response.isArray()) {
            response.forEach(items::add);
        } else if (response.has("data") && response.get("data").isArray()) {
            response.get("data").forEach(items::add);
        } else if (response.has("consignment")) {
            items.add(response.get("consignment"));
        } else {
            items.add(response);
        }

        for (JsonNode item : items) {
            String invoiceNo = item.has("invoice") ? item.get("invoice").asText() : null;

            if (item.has("consignment_id") && item.has("tracking_code")) {
                // Steadfast sometimes omits the invoice on the single API; with one parcel sent, match it directly.
                Optional<BookingRequest> match = sent.stream()
                        .filter(p -> (invoiceNo == null && sent.size() == 1)
                                || p.invoice().getInvoiceNumber().equals(invoiceNo))
                        .findFirst();
                match.ifPresent(p -> out.add(BookingResult.ok(
                        p.orderId(), item.get("consignment_id").asText(), item.get("tracking_code").asText(), null)));
            } else {
                System.err.println("Steadfast failed or missing data for invoice: " + (invoiceNo != null ? invoiceNo : "Unknown"));
                sent.stream()
                        .filter(p -> invoiceNo != null && p.invoice().getInvoiceNumber().equals(invoiceNo))
                        .findFirst()
                        .ifPresent(p -> out.add(BookingResult.failed(p.orderId(), "Steadfast rejected this parcel")));
            }
        }
        return out;
    }

    @Override
    public Optional<CourierStatus> fetchStatus(String consignmentId, String invoiceNumber) {
        JsonNode r = client.getStatusByCid(consignmentId);
        String raw = r != null && r.hasNonNull("delivery_status") ? r.get("delivery_status").asText() : null;
        if (raw == null) return Optional.empty();

        DeliveryStatus mapped = mapStatus(raw);
        LocalDateTime pickedUpAt = null;

        boolean notAttemptedYet = raw.equals("pending") || raw.equals("in_review") || raw.equals("hold");
        if (pickupDetection && notAttemptedYet && invoiceNumber != null) {
            try {
                JsonNode t = client.getTrackingByInvoice(invoiceNumber);
                if (t != null && t.has("tracking") && t.get("tracking").isArray()) {
                    for (JsonNode step : t.get("tracking")) {
                        if (step.path("tracking_type").asInt(1) > 1) {
                            mapped = DeliveryStatus.IN_TRANSIT;
                            pickedUpAt = parseTime(step.path("created_at").asText(null));
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {
                // tracking is a bonus; keep the plain status if it fails
            }
        }
        return Optional.of(new CourierStatus(mapped, raw, pickedUpAt));
    }

    /** Steadfast delivery_status -> DeliveryStatus. Rider-only claims (*_approval_pending) change nothing. */
    public static DeliveryStatus mapStatus(String raw) {
        if (raw == null) return null;
        if (raw.endsWith("_approval_pending")) return null;
        return switch (raw) {
            case "pending", "in_review", "hold" -> DeliveryStatus.PENDING;
            case "delivered", "partial_delivered" -> DeliveryStatus.DELIVERED;
            case "cancelled" -> DeliveryStatus.RETURNED; // confirmed not delivered; the parcel comes back
            default -> null; // exceptional, unknown, partial_*_return_*: keep as is, raw status is stored
        };
    }

    private static LocalDateTime parseTime(String iso) {
        if (iso == null) return null;
        try {
            return OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }
}

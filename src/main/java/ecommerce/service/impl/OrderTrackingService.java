package ecommerce.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import ecommerce.dto.tracking.OrderTrackingDto;
import ecommerce.dto.tracking.OrderTrackingDto.Event;
import ecommerce.dto.tracking.OrderTrackingDto.Step;
import ecommerce.entity.Delivery;
import ecommerce.entity.Order;
import ecommerce.enums.DeliveryStatus;
import ecommerce.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Builds the order journey: our own milestones with real timestamps, plus the courier's event log when it offers one. */
@Service
@RequiredArgsConstructor
public class OrderTrackingService {

    private static final Logger log = LoggerFactory.getLogger(OrderTrackingService.class);
    private static final long EVENT_CACHE_MS = 2 * 60 * 1000L;

    private final SteadfastService steadfast;
    private final ecommerce.repository.DeliveryEventRepository deliveryEvents;
    private final Map<String, Object[]> eventCache = new ConcurrentHashMap<>(); // invoice -> [time, List<Event>]

    public OrderTrackingDto build(Order o, boolean admin) {
        Delivery d = o.getDelivery();
        OrderStatus os = o.getOrderStatus() != null ? o.getOrderStatus() : OrderStatus.PENDING;
        DeliveryStatus ds = d != null ? d.getDeliveryStatus() : null;
        String invoice = o.getInvoice() != null ? o.getInvoice().getInvoiceNumber() : null;

        boolean cancelled = os == OrderStatus.CANCELLED || ds == DeliveryStatus.CANCELLED;
        boolean returned = !cancelled && (os == OrderStatus.RETURNED || ds == DeliveryStatus.RETURNED);
        boolean delivered = !cancelled && !returned && (os == OrderStatus.DELIVERED || ds == DeliveryStatus.DELIVERED);

        boolean confirmed = os != OrderStatus.PENDING && os != OrderStatus.CANCELLED || d != null;
        boolean packed = os == OrderStatus.PACKAGED || os == OrderStatus.SHIPPED || os == OrderStatus.DELIVERED
                || os == OrderStatus.RETURNED || d != null;
        boolean booked = d != null;
        boolean pickedUp = d != null && (d.getPickedUpAt() != null || ds == DeliveryStatus.IN_TRANSIT
                || ds == DeliveryStatus.DELIVERED || ds == DeliveryStatus.RETURNED);

        LocalDateTime deliveredAt = d != null && d.getDeliveredAt() != null ? d.getDeliveredAt() : o.getDeliveredAt();
        LocalDateTime bookedAt = d != null && d.getRequestedAt() != null ? d.getRequestedAt() : o.getShippedAt();
        String courier = d != null ? d.getCourierName() : null;

        List<Event> courierEvents = courierEvents(courier, invoice);
        LocalDateTime courierPickupTime = firstPickupTime(courierEvents);

        List<Step> steps = new ArrayList<>();
        steps.add(new Step("placed", "Order placed", "We received your order", "done", o.getCreatedAt()));
        steps.add(new Step("confirmed", "Confirmed", "Your order was approved", confirmed ? "done" : "upcoming",
                confirmed ? (o.getConfirmedAt() != null ? o.getConfirmedAt() : (o.getPackagedAt() != null ? o.getPackagedAt() : null)) : null));
        steps.add(new Step("packed", "Packed", "Items packed and ready to ship", packed ? "done" : "upcoming", o.getPackagedAt()));
        steps.add(new Step("booked", "Handed to courier",
                booked ? ("Booked with " + (courier != null ? courier : "courier")) : "Waiting for a courier",
                booked ? "done" : "upcoming", booked ? bookedAt : null));
        steps.add(new Step("picked", "Picked up by rider",
                pickedUp ? "The rider has your parcel" : "Rider has not collected it yet",
                pickedUp ? "done" : "upcoming", pickedUp && d != null ? (courierPickupTime != null ? courierPickupTime : d.getPickedUpAt()) : null));

        if (cancelled) {
            steps.add(new Step("cancelled", "Cancelled", "This order was cancelled", "failed", o.getCancelledAt()));
        } else if (returned) {
            steps.add(new Step("returned", "Returned", "The parcel came back to us", "failed", d != null ? d.getReturnedAt() : null));
        } else {
            steps.add(new Step("delivered", "Delivered",
                    delivered ? "Successfully delivered" : "On its way to you",
                    delivered ? "done" : "upcoming", delivered ? deliveredAt : null));
        }

        // First step that is not done becomes "current" (unless the journey has failed).
        if (!cancelled && !returned) {
            for (Step s : steps) {
                if (s.getState().equals("upcoming")) { s.setState("current"); break; }
            }
        }

        int doneCount = (int) steps.stream().filter(s -> s.getState().equals("done")).count();
        int pct = cancelled || returned ? Math.max(10, doneCount * 100 / steps.size()) : doneCount * 100 / steps.size();
        if (delivered) pct = 100;

        String headline;
        String tone;
        if (cancelled) { headline = "Cancelled"; tone = "danger"; }
        else if (returned) { headline = "Returned to sender"; tone = "danger"; }
        else if (delivered) { headline = "Delivered"; tone = "success"; }
        else if (pickedUp) { headline = "On its way to you"; tone = "progress"; }
        else if (booked) { headline = "Waiting for the rider to collect"; tone = "progress"; }
        else if (packed) { headline = "Packed, getting ready to ship"; tone = "progress"; }
        else if (confirmed) { headline = "Confirmed"; tone = "progress"; }
        else { headline = "Order received"; tone = "neutral"; }

        LocalDateTime finishedAt = delivered ? deliveredAt : (returned && d != null ? d.getReturnedAt() : null);
        LocalDateTime end = finishedAt != null ? finishedAt : LocalDateTime.now();
        Long hours = o.getCreatedAt() != null ? Math.max(0, Duration.between(o.getCreatedAt(), end).toHours()) : null;

        OrderTrackingDto dto = new OrderTrackingDto();
        dto.setOrderId(o.getId());
        dto.setInvoice(invoice);
        dto.setOrderStatus(os.name());
        dto.setDeliveryStatus(ds != null ? ds.name() : null);
        dto.setHeadline(headline);
        dto.setTone(tone);
        dto.setProgressPct(pct);
        dto.setPlacedAt(o.getCreatedAt());
        dto.setFinishedAt(finishedAt);
        dto.setHoursInJourney(hours);
        dto.setCourier(courier);
        dto.setConsignmentId(d != null ? d.getConsignmentId() : null);
        dto.setTrackingCode(d != null ? d.getTrackingCode() : null);
        dto.setCodAmount(d != null ? d.getCodAmount() : null);
        dto.setPaymentMethod(o.getPaymentMethod() != null ? o.getPaymentMethod().name() : null);
        dto.setRecipientName(o.getName());
        dto.setRecipientAddress(joinAddress(o));
        dto.setSteps(steps);
        dto.setCourierEvents(courierEvents);
        if (d != null && d.getId() != null) {
            List<Event> hist = new ArrayList<>();
            for (ecommerce.entity.DeliveryEvent e : deliveryEvents.findByDeliveryIdOrderByOccurredAtAsc(d.getId())) {
                hist.add(new Event(e.getRawStatus(), e.getOccurredAt() != null ? e.getOccurredAt().toString() : null, null));
            }
            dto.setStatusHistory(hist);
            dto.setRiderName(d.getRiderName());
            dto.setRiderPhone(d.getRiderPhone());
        }
        if (admin && d != null) {
            dto.setCourierStatus(d.getCourierStatus());
            dto.setLastSyncedAt(d.getLastSyncedAt());
            dto.setDeliveryFee(d.getDeliveryFee());
        }
        return dto;
    }

    /** Steadfast step 1 is "consignment created"; the first later step is the parcel moving, which is when it was collected. */
    private static LocalDateTime firstPickupTime(List<Event> events) {
        for (Event e : events) {
            if (e.getType() != null && e.getType() > 1 && e.getTime() != null) {
                try {
                    return java.time.OffsetDateTime.parse(e.getTime()).atZoneSameInstant(java.time.ZoneId.systemDefault()).toLocalDateTime();
                } catch (Exception ignored) { /* unparseable time: leave the recorded one */ }
            }
        }
        return null;
    }

    private static String joinAddress(Order o) {
        List<String> parts = new ArrayList<>();
        for (String s : new String[]{o.getShippingAddress(), o.getArea(), o.getCity()}) {
            if (s != null && !s.isBlank()) parts.add(s.trim());
        }
        return String.join(", ", parts);
    }

    /** Steadfast publishes a timestamped event list per invoice. Other couriers: none, so the milestones carry the story. */
    @SuppressWarnings("unchecked")
    private List<Event> courierEvents(String courier, String invoice) {
        if (invoice == null || courier == null || !courier.equalsIgnoreCase("Steadfast")) return List.of();
        Object[] cached = eventCache.get(invoice);
        long now = System.currentTimeMillis();
        if (cached != null && now - (Long) cached[0] < EVENT_CACHE_MS) return (List<Event>) cached[1];
        List<Event> out = new ArrayList<>();
        try {
            JsonNode t = steadfast.getTrackingByInvoice(invoice);
            JsonNode arr = t != null ? t.get("tracking") : null;
            if (arr != null && arr.isArray()) {
                for (JsonNode n : arr) {
                    String text = n.hasNonNull("text") ? n.get("text").asText() : null;
                    if (text != null) out.add(new Event(text, n.hasNonNull("created_at") ? n.get("created_at").asText() : null,
                            n.hasNonNull("tracking_type") ? n.get("tracking_type").asInt() : null));
                }
            }
            eventCache.put(invoice, new Object[]{now, out});
        } catch (Exception e) {
            log.debug("Courier events unavailable for {}: {}", invoice, e.getMessage());
            return cached != null ? (List<Event>) cached[1] : List.of();
        }
        return out;
    }
}

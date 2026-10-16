package ecommerce.service.impl;

import ecommerce.courier.CourierStatus;
import ecommerce.entity.Delivery;
import ecommerce.entity.Order;
import ecommerce.enums.DeliveryStatus;
import ecommerce.enums.OrderStatus;
import ecommerce.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Writes a courier's reported status onto a Delivery (and its Order) in its own transaction. */
@Component
@RequiredArgsConstructor
public class DeliveryStatusApplier {

    private final DeliveryRepository deliveryRepository;
    private final ecommerce.repository.DeliveryEventRepository eventRepository;

    private static boolean terminal(DeliveryStatus s) {
        return s == DeliveryStatus.DELIVERED || s == DeliveryStatus.RETURNED || s == DeliveryStatus.CANCELLED;
    }

    private static int rank(DeliveryStatus s) {
        if (s == null) return 0;
        return switch (s) {
            case PENDING -> 0;
            case READY_FOR_PICKUP -> 1;
            case IN_TRANSIT -> 2;
            default -> 3;
        };
    }

    public boolean apply(Long deliveryId, CourierStatus cs) {
        return apply(deliveryId, cs, "SYNC", null, null);
    }

    /**
     * @param source "WEBHOOK" or "SYNC", kept in the status history
     * @param riderName / riderPhone optional, stored only when the courier supplied them
     * @return true if the delivery status actually changed.
     */
    @Transactional
    public boolean apply(Long deliveryId, CourierStatus cs, String source, String riderName, String riderPhone) {
        Delivery d = deliveryRepository.findById(deliveryId).orElse(null);
        if (d == null) return false;

        LocalDateTime now = LocalDateTime.now();

        // Status history: one row each time the courier's own wording changes.
        if (cs.raw() != null && !cs.raw().equals(d.getCourierStatus())) {
            ecommerce.entity.DeliveryEvent ev = new ecommerce.entity.DeliveryEvent();
            ev.setDeliveryId(d.getId());
            ev.setRawStatus(cs.raw());
            ev.setMappedStatus(cs.status() != null ? cs.status().name() : null);
            ev.setSource(source);
            ev.setOccurredAt(now);
            eventRepository.save(ev);
        }

        if (riderName != null && !riderName.isBlank()) d.setRiderName(riderName.trim());
        if (riderPhone != null && !riderPhone.isBlank()) d.setRiderPhone(riderPhone.trim());
        if ((riderName != null && !riderName.isBlank()) || (riderPhone != null && !riderPhone.isBlank())) d.setRiderUpdatedAt(now);

        d.setCourierStatus(cs.raw());
        d.setLastSyncedAt(now);

        DeliveryStatus cur = d.getDeliveryStatus();
        DeliveryStatus next = cs.status();
        boolean changed = false;

        // Never move a finished parcel, and never step backwards (e.g. in transit -> pending).
        if (next != null && next != cur && !terminal(cur) && (terminal(next) || rank(next) > rank(cur))) {
            d.setDeliveryStatus(next);
            changed = true;

            Order o = d.getOrder();
            boolean orderCancelled = o != null && o.getOrderStatus() == OrderStatus.CANCELLED;

            switch (next) {
                case IN_TRANSIT -> {
                    if (d.getPickedUpAt() == null) d.setPickedUpAt(cs.pickedUpAt() != null ? cs.pickedUpAt() : now);
                }
                case DELIVERED -> {
                    if (d.getPickedUpAt() == null) d.setPickedUpAt(cs.pickedUpAt() != null ? cs.pickedUpAt() : now);
                    if (d.getDeliveredAt() == null) d.setDeliveredAt(now);
                    if (o != null && !orderCancelled) {
                        o.setOrderStatus(OrderStatus.DELIVERED);
                        if (o.getDeliveredAt() == null) o.setDeliveredAt(now);
                        o.setDelivered(true);
                    }
                }
                case RETURNED -> {
                    d.setReturnedAt(now);
                    if (o != null && !orderCancelled) o.setOrderStatus(OrderStatus.RETURNED);
                }
                default -> { /* READY_FOR_PICKUP, CANCELLED: delivery only */ }
            }
        }
        deliveryRepository.save(d);
        return changed;
    }
}

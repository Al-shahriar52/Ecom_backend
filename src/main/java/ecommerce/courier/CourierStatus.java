package ecommerce.courier;

import ecommerce.enums.DeliveryStatus;

import java.time.LocalDateTime;

/**
 * What a courier says about a parcel right now.
 * status is our normalised DeliveryStatus, or null when the courier's word shouldn't change anything yet
 * (e.g. Steadfast "*_approval_pending", which is the rider's claim, not confirmed). raw is always kept.
 */
public record CourierStatus(DeliveryStatus status, String raw, LocalDateTime pickedUpAt) {
}

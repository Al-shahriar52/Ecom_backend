package ecommerce.courier;

/** Result of a pickup request. status: SUBMITTED | ALREADY_PENDING | FAILED */
public record PickupOutcome(String status, String pickupId, String message) {
}

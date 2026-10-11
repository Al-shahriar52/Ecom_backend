package ecommerce.courier;

/** Outcome of booking one parcel. */
public record BookingResult(
        Long orderId,
        boolean success,
        String consignmentId,
        String trackingCode,
        /** Delivery fee the courier quoted, when it returns one (Pathao does). */
        Double deliveryFee,
        String message) {

    public static BookingResult ok(Long orderId, String consignmentId, String trackingCode, Double fee) {
        return new BookingResult(orderId, true, consignmentId, trackingCode, fee, null);
    }

    public static BookingResult failed(Long orderId, String message) {
        return new BookingResult(orderId, false, null, null, null, message);
    }
}

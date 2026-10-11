package ecommerce.courier;

/** What a courier can do. The UI and services check these instead of assuming every courier works like Steadfast. */
public record CourierCapabilities(
        boolean singleBooking,
        boolean bulkBooking,
        /** Bulk results come back immediately (Steadfast) vs. processed later (Pathao). */
        boolean bulkIsSynchronous,
        /** A separate "send a rider" call exists (Steadfast). If false, pickup happens automatically from the store. */
        boolean pickupRequest,
        boolean priceQuote,
        boolean statusLookup) {
}

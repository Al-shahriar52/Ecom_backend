package ecommerce.courier;

import java.util.List;
import java.util.Optional;

/**
 * One courier company (Steadfast, Pathao, ...). Everything courier-specific - auth, payload shape,
 * response parsing, bulk behaviour - lives in the adapter. Services only talk to this interface.
 */
public interface CourierProvider {

    /** Stable lowercase key stored in courier_account.provider, e.g. "steadfast". */
    String key();

    /** Name stored in Delivery.courierName and shown in the UI, e.g. "Steadfast". */
    String displayName();

    CourierCapabilities capabilities();

    /** False until credentials are present, e.g. Pathao env vars not set. */
    default boolean isConfigured() { return true; }

    /** True when talking to the courier's test environment. */
    default boolean isSandbox() { return false; }

    /**
     * Books the given parcels. Returns one result per parcel the courier accepted or rejected.
     * Parcels missing from the result are treated as not booked. Implementations decide single vs bulk.
     */
    List<BookingResult> book(List<BookingRequest> parcels);

    /** Only called when capabilities().pickupRequest() is true. */
    default PickupOutcome requestPickup(PickupSpec spec) {
        throw new UnsupportedOperationException(displayName() + " does not use pickup requests");
    }

    /**
     * Current status of a booked parcel. Only called when capabilities().statusLookup() is true.
     * @param invoiceNumber our own invoice id, for couriers that can look up by it
     */
    default Optional<CourierStatus> fetchStatus(String consignmentId, String invoiceNumber) {
        throw new UnsupportedOperationException(displayName() + " does not support status lookup");
    }
}

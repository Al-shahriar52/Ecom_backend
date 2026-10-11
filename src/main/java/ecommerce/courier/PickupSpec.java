package ecommerce.courier;

/** Provider-neutral "send a rider" request. Fields a courier doesn't need are ignored by its adapter. */
public record PickupSpec(Long addressId, Long policeStationId, String address,
                         String contactNumber, String note, Integer estimQty) {
}

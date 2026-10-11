package ecommerce.courier;

import ecommerce.entity.Invoice;
import ecommerce.entity.Order;

/** One parcel to book with a courier. */
public record BookingRequest(Order order, Invoice invoice) {
    public Long orderId() { return order.getId(); }
}

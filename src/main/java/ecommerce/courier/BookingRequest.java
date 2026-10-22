package ecommerce.courier;

import ecommerce.entity.Invoice;
import ecommerce.entity.Order;

/**
 * One parcel to book with a courier. codOverride, when set, replaces the invoice total as the amount
 * the rider collects (the invoice and order itself are not changed).
 */
public record BookingRequest(Order order, Invoice invoice, Double codOverride) {

    public BookingRequest(Order order, Invoice invoice) { this(order, invoice, null); }

    public Long orderId() { return order.getId(); }

    /** Amount the courier should collect: 0 for prepaid, the override if given, otherwise the invoice total. */
    public double cod() {
        if (order.getPaymentMethod() != ecommerce.enums.PaymentMethod.COD) return 0.0;
        if (codOverride != null) return codOverride;
        return invoice.getTotalAmount() != null ? invoice.getTotalAmount() : 0.0;
    }
}

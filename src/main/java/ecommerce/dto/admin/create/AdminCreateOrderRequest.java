package ecommerce.dto.admin.create;

import ecommerce.enums.OrderStatus;
import ecommerce.enums.PaymentMethod;
import ecommerce.enums.PaymentStatus;
import lombok.Data;

import java.util.List;

/** An order typed in by an admin (phone / WhatsApp / walk-in orders). */
@Data
public class AdminCreateOrderRequest {
    private Long customerId;            // set when an existing customer was picked
    private String name;
    private String phone;
    private String email;               // optional

    private String shippingAddress;     // street / house
    private String city;
    private String area;

    private PaymentMethod paymentMethod = PaymentMethod.COD;
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;   // PENDING or PAID
    private OrderStatus orderStatus = OrderStatus.CONFIRMED;       // PENDING or CONFIRMED

    private Double shippingCost;        // null = standard rule (Dhaka 60, elsewhere 120)
    private Double discount;            // flat discount in taka
    private String orderNote;

    private boolean notifyCustomer;     // email + SMS confirmation
    private boolean saveAddress;        // add to the customer's address book

    private List<Item> items;

    @Data
    public static class Item {
        private Long productId;
        private int quantity;
        private Double unitPrice;       // optional price override for this line
    }
}

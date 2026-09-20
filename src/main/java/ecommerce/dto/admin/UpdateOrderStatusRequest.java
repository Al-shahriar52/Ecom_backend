package ecommerce.dto.admin;

import lombok.Data;

/**
 * Full order-edit payload for PATCH /api/v1/admin/orders/{orderId}/status.
 * Every field is optional - only non-null fields are applied, so the
 * frontend can send a partial update. Enum fields must match the backend
 * enum constant names exactly (see ecommerce.enums.*).
 */
@Data
public class UpdateOrderStatusRequest {

    // --- Status ---
    /** ecommerce.enums.OrderStatus: PENDING, CONFIRMED, PACKAGED, SHIPPED, DELIVERED, CANCELLED, RETURNED */
    private String orderStatus;
    /** ecommerce.enums.DeliveryStatus: PENDING, READY_FOR_PICKUP, IN_TRANSIT, DELIVERED, RETURNED, CANCELLED */
    private String deliveryStatus;

    // --- Contact ---
    private String customerName;
    private String phone;
    private String email;
    private String shippingAddress;
    private String city;
    private String area;

    // --- Payment ---
    /** ecommerce.enums.PaymentMethod: COD, BKASH, CARD, NAGAD, ROCKET */
    private String paymentMethod;
    /** ecommerce.enums.PaymentStatus: PENDING, PAID, FAILED, REFUNDED */
    private String paymentStatus;

    // --- Financials ---
    private Double shippingCost;
    private Double totalAmount;

    // --- Courier (manual correction only - normally system-managed) ---
    private String cid;
    private String trackingCode;

    // --- Misc ---
    private String orderNote;

    /** Optional internal note. Not currently persisted (no admin-note column exists yet) - accepted for forward compatibility. */
    private String note;
}
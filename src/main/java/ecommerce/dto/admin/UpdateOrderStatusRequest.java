package ecommerce.dto.admin;

import lombok.Data;

@Data
public class UpdateOrderStatusRequest {
    /** Must match ecommerce.enums.OrderStatus exactly, e.g. PENDING, CONFIRMED, PACKAGED, SHIPPED, DELIVERED, CANCELLED, RETURNED */
    private String orderStatus;

    /** Optional. Must match ecommerce.enums.DeliveryStatus exactly, e.g. PENDING, READY_FOR_PICKUP, IN_TRANSIT, DELIVERED, RETURNED, CANCELLED */
    private String deliveryStatus;

    /** Optional internal note. Not currently persisted (no admin-note column exists yet) - accepted for forward compatibility. */
    private String note;
}

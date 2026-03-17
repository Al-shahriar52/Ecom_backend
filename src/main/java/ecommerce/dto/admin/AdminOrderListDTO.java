package ecommerce.dto.admin;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AdminOrderListDTO {
    private String invoice;          // From Invoice entity
    private LocalDateTime date;      // From Order entity (createdAt)
    private String customer;         // From Order entity (name)
    private String phone;            // From Order entity (phoneNumber)
    private Double totalAmount;      // From Order entity
    private String paymentMethod;    // From Order entity
    private String paymentStatus;    // From Order entity
    private String orderStatus;      // From Order entity
    private String deliveryStatus;   // From Delivery entity
    private String cid;              // From Delivery entity (consignmentId)
    private String email;
    private String address;
    private Long id;
}
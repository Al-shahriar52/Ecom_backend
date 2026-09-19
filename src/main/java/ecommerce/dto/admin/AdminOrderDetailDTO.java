package ecommerce.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminOrderDetailDTO {
    private Long id;
    private String invoice;
    private LocalDateTime date;
    private String customer;
    private String phone;
    private String email;
    private String address;
    private Double totalAmount;
    private Double shippingCost;
    private String paymentMethod;
    private String paymentStatus;
    private String orderStatus;
    private String deliveryStatus;
    private String cid;
    private String trackingCode;
    private String orderNote;
    private List<AdminOrderItemDTO> items;
}

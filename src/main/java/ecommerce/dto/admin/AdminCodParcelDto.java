package ecommerce.dto.admin;

import lombok.Data;

@Data
public class AdminCodParcelDto {
    private Long orderId;
    private String invoice;
    private String consignmentId;
    private String courierName;
    private String customer;
    private double collectable;
    private String deliveryStatus;
}

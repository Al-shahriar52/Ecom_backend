package ecommerce.dto.admin;

import lombok.Data;

@Data
public class AdminOrderItemDTO {
    private Long productId;
    private String productName;
    private String productImage;
    private int quantity;
    private double price;
    private double subtotal;
}

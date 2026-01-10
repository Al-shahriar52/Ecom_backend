package ecommerce.dto.order;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderConfirmationResponse {
    private Long id;
    private OrderUserDTO user;
    private String shippingAddress;
    private String city;
    private String area;
    private String phoneNumber;
    private String email;
    private String orderNote;
    private String paymentMethod;
    private String orderStatus;

    private Double shippingCost;
    private Double totalAmount;
    
    private LocalDateTime createdAt;
    
    private List<OrderItemResponse> orderItems;

    @Data
    public static class OrderUserDTO {
        private String name;
        private String email;
        private String phone;
    }

    @Data
    public static class OrderItemResponse {
        private Long productId;
        private String productName;
        private String productImageUrl;
        private int quantity;
        private Double price;
        private Double total;
    }
}
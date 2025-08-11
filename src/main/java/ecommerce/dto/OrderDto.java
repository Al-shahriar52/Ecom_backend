package ecommerce.dto;

import ecommerce.entity.PayMethod;
import lombok.Data;

import java.util.List;

@Data
public class OrderDto {

    private Long id;
    private PayMethod paymentMethod;
    private double taxPrice;
    private double shippingPrice;
    private double totalPrice;
    private boolean isPaid;
    private String paidAt;
    private boolean isDelivered;
    private String deliveredAt;
    private String createdAt;
    private List<OrderItemDto> orderItems;
    private UserDto user;
    private ShippingDto shipping;
}

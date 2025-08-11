package ecommerce.dto;

import lombok.Data;

@Data
public class OrderItemDto {

    private Long id;
    private String name;
    private int quantity;
    private double price;
    private String image;
}

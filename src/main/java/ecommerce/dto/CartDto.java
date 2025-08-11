package ecommerce.dto;

import lombok.Data;

@Data
public class CartDto {

    private Long id;
    private Long quantity;
    private UserDto user;
    private ProductDto product;
}

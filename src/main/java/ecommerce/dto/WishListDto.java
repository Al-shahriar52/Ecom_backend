package ecommerce.dto;

import lombok.Data;

@Data
public class WishListDto {

    private Long id;
    private UserDto user;
    private ProductDto product;
}

package ecommerce.dto;

import lombok.Data;

@Data
public class WishListDto {

    private Long wishId;
    private Long productId;
    private String imageUrl;
    private String productName;
    private double price;
}

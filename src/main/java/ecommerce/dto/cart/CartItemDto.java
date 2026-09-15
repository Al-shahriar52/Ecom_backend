package ecommerce.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CartItemDto {
    private Long productId;
    private Long cartItemId;
    private String name;
    private int quantity;
    private double price;
    private double regularPrice;
    private double itemTotalPrice;
    private String imageUrl;
    private int stockQuantity;
    private String slug;

    public CartItemDto(Long productId, String name, double price, double regularPrice, String imageUrl, String slug) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.regularPrice = regularPrice;
        this.imageUrl = imageUrl;
        this.slug = slug;
    }
}

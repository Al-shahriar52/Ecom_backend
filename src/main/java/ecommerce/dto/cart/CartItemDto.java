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
    private double itemTotalPrice;
    private String imageUrl;

    public CartItemDto(Long productId, String name, double price, String imageUrl) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
    }
}

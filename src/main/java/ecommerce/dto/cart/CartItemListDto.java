package ecommerce.dto.cart;

import lombok.Data;

import java.util.List;

@Data
public class CartItemListDto {
    private double totalPrice;
    private List<CartItemDto> items;
}

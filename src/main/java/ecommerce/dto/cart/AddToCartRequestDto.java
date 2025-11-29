package ecommerce.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddToCartRequestDto {

    @Min(1)
    private int quantity;

    @NotNull
    private Long productId;
}

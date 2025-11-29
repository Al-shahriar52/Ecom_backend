package ecommerce.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartUpdateRequestDto {

    @NotNull
    private Long cartItemId;
    @NotNull
    private Integer quantity;
}

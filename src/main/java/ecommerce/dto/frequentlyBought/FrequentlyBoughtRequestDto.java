package ecommerce.dto.frequentlyBought;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FrequentlyBoughtRequestDto {

    @NotNull
    private Long mainProductId;

    @NotNull
    private Long pairedProductId;
}

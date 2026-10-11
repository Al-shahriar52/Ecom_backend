package ecommerce.dto.courier;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePickupRequestDto {
    @NotNull
    private Long locationId;
    @Size(max = 500)
    private String note;
    @Min(1) @Max(100000)
    private Integer estimQty;
}

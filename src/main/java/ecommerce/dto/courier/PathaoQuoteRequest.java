package ecommerce.dto.courier;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PathaoQuoteRequest {
    /** Pathao's own store id. Omit to use the default store. */
    private Long storeId;
    @NotNull private Long cityId;
    @NotNull private Long zoneId;
    @DecimalMin("0.5") @DecimalMax("10")
    private double weight = 0.5;
    /** 48 normal, 12 on demand */
    private int deliveryType = 48;
}

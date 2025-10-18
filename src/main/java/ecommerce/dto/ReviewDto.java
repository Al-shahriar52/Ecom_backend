package ecommerce.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ReviewDto {

    private Long productId;
    @Min(value = 1)
    @Max(value = 5)
    private Double rating;
    private String comment;
}

package ecommerce.dto.filter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BrandFilterDTO {
    private Long brandId;
    private String brandName;
    private Long productCount;
}
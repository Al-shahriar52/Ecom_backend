package ecommerce.dto.filter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubCategoryFilterDTO {
    private Long subCategoryId;
    private String subCategoryName;
    private Long productCount;
}
package ecommerce.dto.filter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class CategoryFilterDTO {
    private Long categoryId;
    private String categoryName;
    private Long productCount;
    private List<SubCategoryFilterDTO> subCategories;

    public CategoryFilterDTO(Long categoryId, String categoryName, Long productCount) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.productCount = productCount;
    }
}
package ecommerce.dto.filter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FilterDataResponse {
    private Double minPrice;
    private Double maxPrice;
    private List<CategoryFilterDTO> availableCategories;
    private List<TagFilterDTO> availableTags;
    private List<BrandFilterDTO> availableBrands;
}
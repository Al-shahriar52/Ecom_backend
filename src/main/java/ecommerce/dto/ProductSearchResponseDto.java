package ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductSearchResponseDto {
    private Long productId;
    private String name;
    private String description;
    private String brandName;
    private String categoryName;
    private String subCategoryName;
    private String tagName;
    private double originalPrice;
    private double discountedPrice;
    private int quantity;
    private String sku;
    private double rating;
    private Long numReviews;
    private String imageUrl;
}

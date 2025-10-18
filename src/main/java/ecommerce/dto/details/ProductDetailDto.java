package ecommerce.dto.details;

import ecommerce.dto.VariationDto;
import lombok.Data;

import java.util.List;

@Data
public class ProductDetailDto {
    private Long productId;
    private String name;
    private String description;
    private String brandName;
    private String categoryNames;
    private String tagNames;
    private double originalPrice;
    private double discountedPrice;
    private int quantity;
    private String sku;
    private Double rating;
    private Long numReviews;
    private List<String> imageUrls;
    private List<VariationDto> variations;

    public ProductDetailDto(Long productId, String name, String description,
                            String brandName, String categoryName, String tagName,
                            double originalPrice, double discountedPrice, int quantity,
                            String sku, Double rating, Long numReviews) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.brandName = brandName;
        this.categoryNames = categoryName;
        this.tagNames = tagName;
        this.originalPrice = originalPrice;
        this.discountedPrice = discountedPrice;
        this.quantity = quantity;
        this.sku = sku;
        this.rating = rating;
        this.numReviews = numReviews;
    }
}
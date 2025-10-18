package ecommerce.dto;

import ecommerce.entity.ProductStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ProductDto {

    private Long productId;

    @NotNull
    private String name;

    @NotNull
    private String description;

    @NotNull
    private Long brandId;

    @NotNull
    private Long categoryId;

    @NotNull
    private Long subCategoryId;

    @NotNull
    private Long tagId;

    private ProductStatus status;

    private List<VariationDto> variations;

    @NotNull
    private double originalPrice;

    @NotNull
    private double discountedPrice;

    @NotNull
    private int quantity;

    @NotNull
    private String sku;

    private Double rating;

    private Long numReviews;

    private List<String> images;
}

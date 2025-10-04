package ecommerce.service.impl;

import ecommerce.dto.filter.*;
import ecommerce.repository.*;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FilterService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final TagRepository tagRepository;

    private final BrandRepository brandRepository;

    public FilterService(ProductRepository productRepository, CategoryRepository categoryRepository,
                         SubCategoryRepository subCategoryRepository, TagRepository tagRepository,
                         BrandRepository brandRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.tagRepository = tagRepository;
        this.brandRepository = brandRepository;
    }

    public FilterDataResponse getFilters(Long brandId, Double minPrice, Double maxPrice) {
        FilterDataResponse response = new FilterDataResponse();

        // 1. Get initial Min/Max Price for the slider bounds (ignoring price filters)
        productRepository.findPriceRangeByBrandId(brandId).ifPresent(priceRange -> {
            Double rawMinPrice = priceRange.minPrice();
            Double rawMaxPrice = priceRange.maxPrice();
            if (rawMinPrice != null) response.setMinPrice(Math.floor(rawMinPrice / 100.0) * 100.0);
            if (rawMaxPrice != null) response.setMaxPrice(Math.ceil(rawMaxPrice / 100.0) * 100.0);
        });

        // Use the full price range if no price filter is applied yet
        Double effectiveMinPrice = (minPrice == null) ? response.getMinPrice() : minPrice;
        Double effectiveMaxPrice = (maxPrice == null) ? response.getMaxPrice() : maxPrice;

        // 2. Get Categories with counts based on current filters
        List<CategoryFilterDTO> categories = categoryRepository.findCategoriesWithCountAndPrice(brandId, effectiveMinPrice, effectiveMaxPrice);
        categories.forEach(category -> {
            List<SubCategoryFilterDTO> subCategories = subCategoryRepository.findSubCategoriesWithCount(brandId, category.getCategoryId(), effectiveMinPrice, effectiveMaxPrice);
            category.setSubCategories(subCategories);
        });
        response.setAvailableCategories(categories);

        // 3. Get Tags with counts
        List<TagFilterDTO> tags = tagRepository.findTagsWithCountAndPrice(brandId, effectiveMinPrice, effectiveMaxPrice);
        response.setAvailableTags(tags);

        // 4. If no brand is selected, also provide a list of all available brands
        List<BrandFilterDTO> brands = brandRepository.findAllWithProductCount();
        response.setAvailableBrands(brands);

        return response;
    }
}
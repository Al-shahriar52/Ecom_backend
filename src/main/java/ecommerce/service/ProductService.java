package ecommerce.service;

import ecommerce.dto.BrandMenuData;
import ecommerce.dto.ProductDto;
import ecommerce.dto.ProductSearchResponseDto;
import ecommerce.dto.SubCategoryDto;
import ecommerce.dto.pageResponse.ProductResponse;
import ecommerce.entity.Brand;
import ecommerce.entity.Category;
import ecommerce.entity.Tag;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ProductService {

    ProductDto add(ProductDto productDto, MultipartFile[] multipartFile) throws IOException;
    ProductDto getById(Long productId);
    String delete(Long productId);
    ProductDto update(ProductDto productDto, MultipartFile[] files) throws IOException;
    ProductResponse search(int pageNo, int pageSize,
                           String sortBy, String direction,
                           String query, Long category,
                           Long brandId, Long subCategoryId,
                           Double minPrice, Double maxPrice);

    List<Category> categoryList();

    List<SubCategoryDto> subCategoryList(Long categoryId);

    List<Brand> brandList();
    BrandMenuData getBrandMenuData();

    List<Tag> tagList();
    List<ProductSearchResponseDto> findNewestArrivals();
}

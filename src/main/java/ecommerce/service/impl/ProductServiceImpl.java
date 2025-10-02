package ecommerce.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import ecommerce.dto.*;
import ecommerce.dto.pageResponse.ProductResponse;
import ecommerce.entity.*;
import ecommerce.exceptionHandling.ResourceNotFound;
import ecommerce.repository.*;
import ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ModelMapper mapper;
    private final ProductRepository productRepository;
    private final Cloudinary cloudinary;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final ProductImageRepository productImageRepository;
    private final BrandRepository brandRepository;
    private final TagRepository tagRepository;

    @Override
    public ProductDto add(ProductDto productDto, MultipartFile[] files) throws IOException {

        Product product = mapToEntity(productDto);
        List<ProductImage> productImages = uploadFiles(Arrays.asList(files), product);
        Product data = productRepository.save(product);
        productImageRepository.saveAll(productImages);
        return mapToDto(data);
    }

    public String uploadFile(MultipartFile file) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "resource_type", "auto",
                        "quality", "auto",
                        "fetch_format", "auto"     // Converts to modern formats (like WebP/AVIF if supported)
                ));
        return uploadResult.get("secure_url").toString();
    }

    public List<ProductImage> uploadFiles(List<MultipartFile> files, Product product) throws IOException {
        List<ProductImage> productImages = new ArrayList<>();
        for (MultipartFile file : files) {
            String url = uploadFile(file);
            ProductImage productImage = new ProductImage();
            productImage.setProduct(product);
            productImage.setImageUrl(url);
            productImages.add(productImage);
        }
        return productImages;
    }

    @Override
    public ProductDto getById(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(()->
                new ResourceNotFound("Product", "id", productId));

        return mapToDto(product);
    }

    @Override
    public String delete(Long productId) {

        Product product = productRepository.findById(productId).orElseThrow(()->
                new ResourceNotFound("Product", "id", productId));

        productRepository.delete(product);
        return "your product :"+product.getName()+" is successfully deleted.";
    }

    @Override
    public ProductDto update(ProductDto productDto, MultipartFile[] files) throws IOException {

        Product product = productRepository.findById(productDto.getProductId()).orElseThrow(()->
                new ResourceNotFound("Product", "id", productDto.getProductId()));

        Category category = categoryRepository.findById(productDto.getCategoryId()).orElseThrow(() ->
                new ResourceNotFound("Category", "id", productDto.getCategoryId()));

        List<ProductImage> imageUrls = uploadFiles(Arrays.asList(files), product);
        productImageRepository.saveAll(imageUrls);

        Optional<Brand> brand = brandRepository.findById(productDto.getBrandId());
        brand.ifPresent(product::setBrand);
        product.setName(productDto.getName());
        product.setCategory(category);
        product.setQuantity(productDto.getQuantity());
        product.setDescription(productDto.getDescription());
        product.setNumReviews(productDto.getNumReviews());
        product.setOriginalPrice(productDto.getOriginalPrice());
        product.setDiscountedPrice(productDto.getDiscountedPrice());
        product.setRating(productDto.getRating());

        productRepository.save(product);
        return mapToDto(product);
    }

    @Override
    public ProductResponse search(int pageNo, int pageSize, String sortBy,
                                  String direction, String query,
                                  Long categoryId,
                                  Long brandId,
                                  Long subCategoryId,
                                  Double minPrice,
                                  Double maxPrice) {
        Sort sort = direction.equalsIgnoreCase(Sort.Direction.DESC.name())
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<ProductSearchResponseDto> products = productRepository.search(pageable, query, categoryId, brandId, subCategoryId, minPrice, maxPrice);
        return getProductResult(products);
    }

    @Override
    public List<Category> categoryList() {
        return categoryRepository.findAll();
    }

    @Override
    public List<SubCategoryDto> subCategoryList(Long categoryId) {
        return subCategoryRepository.findByCategoryId(categoryId);
    }

    @Override
    public List<Brand> brandList() {
        return brandRepository.findAll();
    }

    @Override
    public List<Tag> tagList() {
        return tagRepository.findAll();
    }

    public ProductResponse getProductResult(Page<ProductSearchResponseDto> products) {

        ProductResponse response = new ProductResponse();
        response.setContent(products.getContent());
        response.setPageNo(products.getNumber());
        response.setPageSize(products.getSize());
        response.setTotalPages(products.getTotalPages());
        response.setTotalElements(products.getTotalElements());
        response.setLast(products.isLast());

        return response;
    }

    @Override
    public BrandMenuData getBrandMenuData() {
        // 1. Make a SINGLE database call to get all necessary data
        List<BrandMenuDTO> allBrandData = brandRepository.findAllBrandMenuData();

        // 2. Process the results in memory

        // Filter the list to get only top brands and map them to the TopBrand DTO
        List<TopBrand> topBrands = allBrandData.stream()
                .filter(BrandMenuDTO::isTop)
                .map(this::mapToTopBrand)
                .collect(Collectors.toList());

        // Map the full list to the AllBrand DTO
        List<AllBrand> allBrands = allBrandData.stream()
                .map(this::mapToAllBrand)
                .collect(Collectors.toList());

        // 3. Assemble the final data object
        BrandMenuData brandMenuData = new BrandMenuData();
        brandMenuData.setTopBrands(topBrands);
        brandMenuData.setAllBrands(allBrands);

        return brandMenuData;
    }

    // Helper method to convert BrandMenuDTO to a TopBrand DTO
    private TopBrand mapToTopBrand(BrandMenuDTO dto) {
        TopBrand topBrand = new TopBrand();
        topBrand.setId(dto.getId());
        topBrand.setName(dto.getName());
        topBrand.setSlug(dto.getSlug());
        topBrand.setLogoUrl(dto.getLogoUrl());
        topBrand.setProductCount(dto.getProductCount()); // Now we have the count
        return topBrand;
    }

    // Helper method to convert BrandMenuDTO to an AllBrand DTO
    private AllBrand mapToAllBrand(BrandMenuDTO dto) {
        AllBrand allBrand = new AllBrand();
        allBrand.setId(dto.getId());
        allBrand.setName(dto.getName());
        allBrand.setSlug(dto.getSlug());
        allBrand.setProductCount(dto.getProductCount());
        return allBrand;
    }

    private String uploadImage(MultipartFile imageFile) {
        return "/upload/" + imageFile.getOriginalFilename();
    }

    public Product mapToEntity(ProductDto productDto) {
        Brand brand = brandRepository.findById(productDto.getBrandId()).orElseThrow(()->
                new ResourceNotFound("Brand", "id", productDto.getBrandId()));

        Category category = categoryRepository.findById(productDto.getCategoryId()).orElseThrow(() ->
                new ResourceNotFound("Category", "id", productDto.getCategoryId()));

        SubCategory subCategory = subCategoryRepository.findById(productDto.getSubCategoryId()).orElseThrow(()->
                new ResourceNotFound("SubCategory", "id", productDto.getSubCategoryId()));

        Optional<Tag> tag = tagRepository.findById(productDto.getTagId());
        Product product = new Product();
        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setSku(productDto.getSku());
        product.setBrand(brand);
        product.setStatus(productDto.getStatus());
        product.setDiscountedPrice(productDto.getDiscountedPrice());
        product.setOriginalPrice(productDto.getOriginalPrice());
        product.setRating(productDto.getRating());
        product.setCategory(category);
        product.setSubCategory(subCategory);
        tag.ifPresent(product::setTag);
        product.setQuantity(productDto.getQuantity());
        return product;
    }

    public ProductDto mapToDto(Product product) {
        return mapper.map(product, ProductDto.class);
    }
}

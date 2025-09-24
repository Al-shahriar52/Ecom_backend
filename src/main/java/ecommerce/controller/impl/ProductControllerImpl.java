package ecommerce.controller.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import ecommerce.controller.ProductController;
import ecommerce.dto.BrandMenuData;
import ecommerce.dto.GenericResponseDto;
import ecommerce.dto.ProductDto;
import ecommerce.dto.SubCategoryDto;
import ecommerce.dto.pageResponse.ProductResponse;
import ecommerce.entity.Brand;
import ecommerce.entity.Category;
import ecommerce.entity.Tag;
import ecommerce.service.ProductService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/product")
public class ProductControllerImpl implements ProductController {

    private final ProductService productService;

    public ProductControllerImpl(ProductService productService) {
        this.productService = productService;
    }

    @Override
    @PostMapping(value = "/add", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> add(@Valid @RequestPart("productDto") String productDtoString, @RequestPart("files") MultipartFile[] files) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        ProductDto productDto = mapper.readValue(productDtoString, ProductDto.class);
        ProductDto product = productService.add(productDto, files);
        return new ResponseEntity<>(GenericResponseDto.success("Product created successfully",product, HttpStatus.CREATED.value()), HttpStatus.CREATED);
    }

    @Override
    @GetMapping("/get/{productId}")
    public ResponseEntity<?> getById(@PathVariable Long productId) {

            ProductDto productDto = productService.getById(productId);
            return new ResponseEntity<>(productDto, HttpStatus.OK);
    }

    @Override
    @DeleteMapping("/delete/{productId}")
    public ResponseEntity<?> delete(@PathVariable Long productId) {
        String message = productService.delete(productId);
        return new ResponseEntity<>(message, HttpStatus.OK);
    }

    @Override
    @PutMapping("/update")
    public ResponseEntity<?> update(@Valid @RequestBody ProductDto productDto, @RequestParam("file") MultipartFile[] files) throws IOException {

        ProductDto productUpdate = productService.update(productDto, files);
        return new ResponseEntity<>(productUpdate, HttpStatus.OK);
    }

    @Override
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam(defaultValue = "0") int pageNo,
                                    @RequestParam(defaultValue = "10") int pageSize,
                                    @RequestParam(defaultValue = "id") String sortBy,
                                    @RequestParam(defaultValue = "ASC") String sortDir,
                                    @RequestParam(defaultValue = "") String query,
                                    @RequestParam(required = false) Long categoryId,
                                    @RequestParam(required = false) Long brandId) {

        ProductResponse productSearch = productService.search(pageNo, pageSize, sortBy, sortDir, query, categoryId, brandId);
        return new ResponseEntity<>(GenericResponseDto.success("Fetch search result successfully", productSearch, HttpStatus.OK.value()), HttpStatus.OK);
    }

    @GetMapping("/categories")
    @Override
    public ResponseEntity<?> categoryList() {
        List<Category> categories = productService.categoryList();
        return new ResponseEntity<>(GenericResponseDto.success("Fetch category successfully", categories, HttpStatus.OK.value()), HttpStatus.OK);
    }

    @GetMapping("/subCategory/{categoryId}")
    @Override
    public ResponseEntity<?> subCategoryList(@PathVariable Long categoryId) {
        List<SubCategoryDto> subCategories = productService.subCategoryList(categoryId);
        return new ResponseEntity<>(GenericResponseDto.success("Fetch sub category successfully", subCategories, HttpStatus.OK.value()), HttpStatus.OK);
    }

    @GetMapping("/brand")
    @Override
    public ResponseEntity<?> brandList() {
        List<Brand> brands = productService.brandList();
        return new ResponseEntity<>(GenericResponseDto.success("Fetch category successfully", brands, HttpStatus.OK.value()), HttpStatus.OK);
    }

    @GetMapping("/brandMenu")
    @Override
    public ResponseEntity<?> getBrandMenu() {
        BrandMenuData data = productService.getBrandMenuData();
        return new ResponseEntity<>(GenericResponseDto.success("Fetch brand menu successfully", data, HttpStatus.OK.value()), HttpStatus.OK);
    }

    @GetMapping("/tag")
    @Override
    public ResponseEntity<?> tagList() {
        List<Tag> tags = productService.tagList();
        return new ResponseEntity<>(GenericResponseDto.success("Fetch category successfully", tags, HttpStatus.OK.value()), HttpStatus.OK);
    }
}

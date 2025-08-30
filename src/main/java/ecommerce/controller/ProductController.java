package ecommerce.controller;

import ecommerce.dto.ProductDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ProductController {

    ResponseEntity<?> add(String productDto, MultipartFile[] files) throws IOException;
    ResponseEntity<?> getById(Long productId);
    ResponseEntity<?> delete(Long productId);
    ResponseEntity<?> update(ProductDto productDto, MultipartFile[] files) throws IOException;
    ResponseEntity<?> search(int pageNo, int pageSize, String sortBy, String query);
    ResponseEntity<?> categoryList();
    ResponseEntity<?> subCategoryList(Long categoryId);
    ResponseEntity<?> brandList();
    ResponseEntity<?> tagList();
}

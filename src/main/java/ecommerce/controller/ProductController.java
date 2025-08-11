package ecommerce.controller;

import ecommerce.dto.ProductDto;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

public interface ProductController {

    ResponseEntity<?> add(ProductDto productDto) throws IOException;
    ResponseEntity<?> getById(Long productId);
    ResponseEntity<?> delete(Long productId);
    ResponseEntity<?> update(Long productId, ProductDto productDto);
    ResponseEntity<?> search(int pageNo, int pageSize, String sortBy, String query);
}

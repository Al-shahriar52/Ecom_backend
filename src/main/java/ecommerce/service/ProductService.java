package ecommerce.service;

import ecommerce.dto.ProductDto;
import ecommerce.dto.pageResponse.ProductResponse;

import java.io.IOException;

public interface ProductService {

    ProductDto add(ProductDto productDto) throws IOException;
    ProductDto getById(Long productId);
    String delete(Long productId);
    ProductDto update(Long productId, ProductDto productDto);
    ProductResponse search(int pageNo, int pageSize, String sortBy, String query);
}

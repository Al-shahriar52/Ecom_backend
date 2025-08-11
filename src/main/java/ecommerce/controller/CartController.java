package ecommerce.controller;

import ecommerce.dto.CartDto;
import org.springframework.http.ResponseEntity;

public interface CartController {

    ResponseEntity<?> add(CartDto cartDto, Long userId, Long productId);
    ResponseEntity<?> getAll(int pageNo, int pageSize, String sortBy);
    ResponseEntity<?> delete(Long cartId);
}

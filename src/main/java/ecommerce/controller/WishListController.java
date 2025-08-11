package ecommerce.controller;

import ecommerce.dto.WishListDto;
import org.springframework.http.ResponseEntity;

public interface WishListController {

    ResponseEntity<?> add(WishListDto wishListDto, Long userId, Long productId);
    ResponseEntity<?> getAll(int pageNo, int pageSize, String sortBy);
    ResponseEntity<?> delete(Long id);
}

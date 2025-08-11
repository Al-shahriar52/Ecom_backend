package ecommerce.service;

import ecommerce.dto.CartDto;
import ecommerce.dto.pageResponse.CartResponse;

public interface CartService {

    CartDto add(CartDto cartDto, Long userId, Long productId);
    CartResponse getAll(int pageNo, int pageSize, String sortBy);
    String delete(Long cartId);
}

package ecommerce.service;

import ecommerce.dto.WishListDto;
import ecommerce.dto.pageResponse.WishListResponse;

public interface WishListService {

    WishListDto add(WishListDto wishListDto, Long userId, Long productId);
    WishListResponse getAll(int pageNo, int pageSize, String sortBy);
    String delete(Long Id);
}

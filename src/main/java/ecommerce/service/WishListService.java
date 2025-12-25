package ecommerce.service;

import ecommerce.dto.pageResponse.WishListResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface WishListService {

    Long add(Long productId, HttpServletRequest servletRequest);
    WishListResponse getAll(HttpServletRequest servletRequest);
    Long delete(Long productId, HttpServletRequest servletRequest);
}

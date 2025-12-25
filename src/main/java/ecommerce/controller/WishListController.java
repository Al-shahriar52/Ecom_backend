package ecommerce.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public interface WishListController {

    ResponseEntity<?> add(Long productId, HttpServletRequest servletRequest);
    ResponseEntity<?> getAll(HttpServletRequest servletRequest);
    ResponseEntity<?> delete(Long productId, HttpServletRequest servletRequest);
}

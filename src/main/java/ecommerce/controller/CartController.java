package ecommerce.controller;

import ecommerce.dto.cart.AddToCartRequestDto;
import ecommerce.dto.cart.CartUpdateRequestDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public interface CartController {

    ResponseEntity<?> add(AddToCartRequestDto requestDto, HttpServletRequest servletRequest);
    ResponseEntity<?> getCart(HttpServletRequest servletRequest);
    ResponseEntity<?> delete(Long cartItemId, HttpServletRequest servletRequest);
    ResponseEntity<?> updateQuantity(CartUpdateRequestDto requestDto, HttpServletRequest servletRequest);
}

package ecommerce.service;

import ecommerce.dto.cart.AddToCartRequestDto;
import ecommerce.dto.cart.CartItemDto;
import ecommerce.dto.cart.CartItemListDto;
import ecommerce.dto.cart.CartUpdateRequestDto;
import ecommerce.dto.pageResponse.CartResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface CartService {

    AddToCartRequestDto addItemToCart(AddToCartRequestDto requestDto, HttpServletRequest servletRequest);
    CartItemListDto getCart(HttpServletRequest servletRequest);
    Long delete(Long cartId, HttpServletRequest servletRequest);
    CartUpdateRequestDto updateQuantity(CartUpdateRequestDto requestDto, HttpServletRequest servletRequest);
}

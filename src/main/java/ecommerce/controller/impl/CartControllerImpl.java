package ecommerce.controller.impl;

import ecommerce.controller.CartController;
import ecommerce.dto.CartDto;
import ecommerce.dto.pageResponse.CartResponse;
import ecommerce.entity.Cart;
import ecommerce.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
public class CartControllerImpl implements CartController {

    private final CartService cartService;

    public CartControllerImpl(CartService cartService) {
        this.cartService = cartService;
    }

    @Override
    @PostMapping("/user/{userId}/product/{productId}/add")
    public ResponseEntity<?> add(@Valid @RequestBody CartDto cartDto,
                                 @PathVariable Long userId,@PathVariable Long productId) {

        CartDto cart = cartService.add(cartDto, userId, productId);
        return new ResponseEntity<>(cart, HttpStatus.CREATED);
    }

    @Override
    @GetMapping("/get/all")
    public ResponseEntity<?> getAll(@RequestParam(defaultValue = "0") int pageNo,
                                    @RequestParam(defaultValue = "10") int pageSize,
                                    @RequestParam(defaultValue = "id") String sortBy) {

        CartResponse cart = cartService.getAll(pageNo, pageSize, sortBy);
        return new ResponseEntity<>(cart, HttpStatus.OK);
    }

    @Override
    @DeleteMapping("/delete/{cartId}")
    public ResponseEntity<?> delete(@PathVariable Long cartId) {

        String message = cartService.delete(cartId);
        return new ResponseEntity<>(message, HttpStatus.OK);
    }
}

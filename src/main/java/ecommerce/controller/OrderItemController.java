package ecommerce.controller;

import ecommerce.dto.OrderItemDto;
import org.springframework.http.ResponseEntity;

public interface OrderItemController {

    ResponseEntity<?> add(OrderItemDto orderItemDto, Long productId);
    ResponseEntity<?> delete(Long itemId);
}

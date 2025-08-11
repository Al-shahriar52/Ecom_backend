package ecommerce.controller.impl;

import ecommerce.controller.OrderItemController;
import ecommerce.dto.OrderItemDto;
import ecommerce.service.OrderItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/item")
public class OrderItemControllerImpl implements OrderItemController {

    private final OrderItemService orderItemService;

    public OrderItemControllerImpl(OrderItemService orderItemService) {
        this.orderItemService = orderItemService;
    }

    @Override
    @PostMapping("/product/{productId}/add")
    public ResponseEntity<?> add(@Valid @RequestBody OrderItemDto orderItemDto,
                                 @PathVariable Long productId) {

        OrderItemDto item = orderItemService.add(orderItemDto, productId);
        return new ResponseEntity<>(item, HttpStatus.CREATED);
    }

    @Override
    @DeleteMapping("/delete/{itemId}")
    public ResponseEntity<?> delete(@PathVariable Long itemId) {

        String message = orderItemService.delete(itemId);
        return new ResponseEntity<>(message, HttpStatus.OK);
    }
}

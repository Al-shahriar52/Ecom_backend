package ecommerce.controller.impl;

import ecommerce.controller.OrderController;
import ecommerce.dto.OrderDto;
import ecommerce.dto.pageResponse.OrderResponse;
import ecommerce.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order")
public class OrderControllerImpl implements OrderController {

    private final OrderService orderService;

    public OrderControllerImpl(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    @PostMapping("/user/{userId}/item/{orderItemId}/add")
    public ResponseEntity<?> add(@Valid @RequestBody OrderDto orderDto,
                                 @PathVariable Long userId,
                                 @PathVariable List<Long> orderItemId) {

        OrderDto order = orderService.add(orderDto, userId, orderItemId);
        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }

    @Override
    @PutMapping("/update/{orderId}")
    public ResponseEntity<?> update(@Valid @RequestBody OrderDto orderDto, @PathVariable Long orderId) {

        OrderDto updatedOrder = orderService.update(orderDto, orderId);
        return new ResponseEntity<>(updatedOrder, HttpStatus.OK);
    }

    @Override
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam(defaultValue = "0") int pageNo,
                                    @RequestParam(defaultValue = "10") int pageSize,
                                    @RequestParam(defaultValue = "id") String sortBy,
                                    @RequestParam(defaultValue = "") String query) {

        OrderResponse response = orderService.search(pageNo, pageSize, sortBy, query);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    @DeleteMapping("/delete/{orderId}")
    public ResponseEntity<?> delete(@PathVariable Long orderId) {

        String message = orderService.delete(orderId);
        return new ResponseEntity<>(message, HttpStatus.OK);
    }
}

package ecommerce.controller.impl;

import ecommerce.controller.OrderController;
import ecommerce.dto.GenericResponseDto;
import ecommerce.dto.OrderDto;
import ecommerce.dto.order.OrderConfirmationResponse;
import ecommerce.dto.order.OrderRequest;
import ecommerce.dto.pageResponse.OrderResponse;
import ecommerce.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/order")
public class OrderControllerImpl implements OrderController {

    private final OrderService orderService;

    public OrderControllerImpl(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    @PostMapping("/placeOrder")
    public ResponseEntity<?> placeOrder(HttpServletRequest servletRequest, @Valid @RequestBody OrderRequest orderRequest) {

        Long order = orderService.placeOrder(servletRequest, orderRequest);
        return new ResponseEntity<>(GenericResponseDto.success("Order placed successfully", order, HttpStatus.CREATED.value()), HttpStatus.CREATED);
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        try {
            OrderConfirmationResponse order = orderService.getOrderById(id);
            return new ResponseEntity<>(GenericResponseDto.success("Order information fetched successfully", order, HttpStatus.OK.value()), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(GenericResponseDto.error("Order fetching error", e.getMessage(), HttpStatus.BAD_REQUEST.value()), HttpStatus.BAD_REQUEST);
        }
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

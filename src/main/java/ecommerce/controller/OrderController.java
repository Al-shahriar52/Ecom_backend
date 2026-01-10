package ecommerce.controller;

import ecommerce.dto.OrderDto;
import ecommerce.dto.order.OrderRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface OrderController {

    ResponseEntity<?> placeOrder(HttpServletRequest servletRequest, OrderRequest orderRequest);
    ResponseEntity<?> getOrderById(Long orderId);
    ResponseEntity<?> update(OrderDto orderDto, Long orderId);
    ResponseEntity<?> search(int pageNo, int pageSize, String sortBy, String query);
    ResponseEntity<?> delete(Long orderId);
}

package ecommerce.controller;

import ecommerce.dto.OrderDto;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface OrderController {

    ResponseEntity<?> add(OrderDto orderDto, Long userId, List<Long> orderItemIds);
    ResponseEntity<?> update(OrderDto orderDto, Long orderId);
    ResponseEntity<?> search(int pageNo, int pageSize, String sortBy, String query);
    ResponseEntity<?> delete(Long orderId);
}

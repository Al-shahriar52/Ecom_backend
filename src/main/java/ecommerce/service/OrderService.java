package ecommerce.service;

import ecommerce.dto.OrderDto;
import ecommerce.dto.pageResponse.OrderResponse;

import java.util.List;
import java.util.Set;

public interface OrderService {

    OrderDto add(OrderDto orderDto, Long userId, List<Long> orderItemIds);
    OrderDto update(OrderDto orderDto, Long orderId);
    OrderResponse search(int pageNo, int pageSize, String sortBy, String query);
    String delete(Long orderId);
}

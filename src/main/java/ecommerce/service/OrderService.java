package ecommerce.service;

import ecommerce.dto.OrderDto;
import ecommerce.dto.order.OrderConfirmationResponse;
import ecommerce.dto.order.OrderRequest;
import ecommerce.dto.pageResponse.OrderResponse;
import ecommerce.entity.Order;
import jakarta.servlet.http.HttpServletRequest;

public interface OrderService {

    Long placeOrder(HttpServletRequest servletRequest, OrderRequest request);
    OrderDto update(OrderDto orderDto, Long orderId);
    OrderResponse search(int pageNo, int pageSize, String sortBy, String query);
    String delete(Long orderId);
    OrderConfirmationResponse getOrderById(Long id);
}

package ecommerce.service;

import ecommerce.dto.OrderItemDto;

public interface OrderItemService {

    OrderItemDto add(OrderItemDto orderItemDto, Long productId);
    String delete(Long itemId);

}

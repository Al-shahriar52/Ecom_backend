package ecommerce.service.impl;

import ecommerce.dto.OrderItemDto;
import ecommerce.entity.OrderItem;
import ecommerce.entity.Product;
import ecommerce.exceptionHandling.ResourceNotFound;
import ecommerce.repository.OrderItemRepository;
import ecommerce.repository.ProductRepository;
import ecommerce.service.OrderItemService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class OrderItemServiceImpl implements OrderItemService {

    private final ModelMapper mapper;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public OrderItemServiceImpl(ModelMapper mapper, OrderItemRepository orderItemRepository, ProductRepository productRepository) {
        this.mapper = mapper;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
    }

    @Override
    public OrderItemDto add(OrderItemDto orderItemDto, Long productId) {
        OrderItem item = mapToEntity(orderItemDto);

        Product product = productRepository.findById(productId).orElseThrow(()->
                new ResourceNotFound("Product", "id", productId));

        item.setName(product.getName());
        item.setPrice(product.getPrice());
        item.setImage(product.getImage());
        item.setQuantity(orderItemDto.getQuantity());
        item.setProduct(product);
        orderItemRepository.save(item);

        return mapToDto(item);
    }

    @Override
    public String delete(Long itemId) {

        OrderItem item = orderItemRepository.findById(itemId).orElseThrow(()->
                new ResourceNotFound("OrderItem", "id", itemId));

        orderItemRepository.delete(item);
        return "Your order item :"+item.getName()+" is deleted successfully.";
    }

    public OrderItem mapToEntity(OrderItemDto itemDto) {
        return mapper.map(itemDto, OrderItem.class);
    }

    public OrderItemDto mapToDto(OrderItem item) {
        return mapper.map(item, OrderItemDto.class);
    }
}

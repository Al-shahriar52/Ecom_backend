package ecommerce.service.impl;

import ecommerce.dto.OrderDto;
import ecommerce.dto.pageResponse.OrderResponse;
import ecommerce.entity.Order;
import ecommerce.entity.OrderItem;
import ecommerce.entity.User;
import ecommerce.exceptionHandling.ResourceNotFound;
import ecommerce.repository.OrderItemRepository;
import ecommerce.repository.OrderRepository;
import ecommerce.repository.UserRepository;
import ecommerce.service.OrderService;
import ecommerce.utils.DateTimeUtil;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderServiceImpl implements OrderService {

    private final ModelMapper mapper;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DateTimeUtil dateTimeUtil;

    public OrderServiceImpl(ModelMapper mapper, UserRepository userRepository,
                            OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                            DateTimeUtil dateTimeUtil) {
        this.mapper = mapper;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.dateTimeUtil = dateTimeUtil;
    }

    @Override
    public OrderDto add(OrderDto orderDto, Long userId, List<Long> orderItemIds) {

        Order order = mapToEntity(orderDto);

        User user = userRepository.findById(userId).orElseThrow(()->
                new ResourceNotFound("User", "id", userId));

        List<OrderItem> orderItems = new ArrayList<>();

        for (Long orderItemId : orderItemIds) {
            Optional<OrderItem> orderItemOptional = orderItemRepository.findById(orderItemId);

            if (orderItemOptional.isPresent()) {
                OrderItem orderItem = orderItemOptional.get();
                orderItems.add(orderItem);
            } else {
                throw new ResourceNotFound("OrderItem", "id", orderItemId);
            }
        }

        String formattedTime = dateTimeUtil.convert();

        try{
            if(orderDto.isPaid()){
                order.setPaid(true);
                order.setPaidAt(formattedTime);
            }else {
                order.setPaid(false);
                order.setPaidAt(null);
            }
        }catch (NullPointerException e) {
            e.getMessage();
        }

        order.setCreatedAt(formattedTime);
        order.setUser(user);
        order.setOrderItems(orderItems);
        orderRepository.save(order);
        return mapToDto(order);
    }

    @Override
    public OrderDto update(OrderDto orderDto, Long orderId) {

        Order order = orderRepository.findById(orderId).orElseThrow(()->
                new ResourceNotFound("Order", "id", orderId));

        String formattedTime = dateTimeUtil.convert();

        try{
            if(orderDto.isDelivered()) {
                order.setDelivered(true);
                order.setDeliveredAt(formattedTime);
            }else {
                order.setDelivered(false);
                order.setDeliveredAt(null);
            }
        }catch (NullPointerException e) {
            e.getMessage();
        }

        orderRepository.save(order);
        return mapToDto(order);
    }

    @Override
    public OrderResponse search(int pageNo, int pageSize, String sortBy, String query) {

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(sortBy));
        Page<Order> orders = orderRepository.search(pageable, query);

        List<Order> orderList = orders.getContent();
        List<OrderDto> content = orderList.stream().map(this::mapToDto).toList();

        OrderResponse response = new OrderResponse();
        response.setContent(content);
        response.setPageNo(orders.getNumber());
        response.setPageSize(orders.getSize());
        response.setTotalPages(orders.getTotalPages());
        response.setTotalElements(orders.getTotalElements());
        response.setLast(orders.isLast());

        return response;
    }

    @Override
    public String delete(Long orderId) {

        Order order = orderRepository.findById(orderId).orElseThrow(()->
                new ResourceNotFound("Order", "id", orderId));
        orderRepository.delete(order);
        return "your order : "+order.getId()+ " is deleted successfully.";
    }

    public Order mapToEntity(OrderDto orderDto) {
        return mapper.map(orderDto, Order.class);
    }

    public OrderDto mapToDto(Order order) {
        return mapper.map(order, OrderDto.class);
    }
}

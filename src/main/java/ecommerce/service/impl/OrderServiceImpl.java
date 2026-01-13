package ecommerce.service.impl;

import ecommerce.dto.OrderDto;
import ecommerce.dto.order.OrderConfirmationResponse;
import ecommerce.dto.order.OrderRequest;
import ecommerce.dto.pageResponse.OrderResponse;
import ecommerce.entity.Order;
import ecommerce.entity.OrderItem;
import ecommerce.entity.Product;
import ecommerce.entity.User;
import ecommerce.enums.OrderStatus;
import ecommerce.enums.PaymentMethod;
import ecommerce.enums.PaymentStatus;
import ecommerce.exceptionHandling.ResourceNotFound;
import ecommerce.repository.*;
import ecommerce.service.OrderService;
import ecommerce.utils.DateTimeUtil;
import ecommerce.utils.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final ModelMapper mapper;
    private final OrderRepository orderRepository;
    private final DateTimeUtil dateTimeUtil;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final TokenUtil tokenUtil;

    @Override
    @Transactional
    public Long placeOrder(HttpServletRequest servletRequest, OrderRequest request) {
        // --- FETCH USER ---
        User user = tokenUtil.extractUserInfo(servletRequest);
        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(request.getShippingAddress());
        order.setCity(request.getCity());
        order.setArea(request.getArea());
        order.setPhoneNumber(request.getPhone());
        order.setEmail(request.getEmail());
        order.setName(request.getName());
        order.setOrderNote(request.getOrderNote());
        order.setPaymentMethod(request.getPaymentMethod());

        // --- 1. CALCULATE SHIPPING ---
        double shippingCharge;

        if (request.getCity() != null && request.getCity().trim().equalsIgnoreCase("Dhaka")) {
            shippingCharge = 60.00;
        } else {
            shippingCharge = 120.00;
        }
        order.setShippingCost(shippingCharge);

        // --- 2. PROCESS ITEMS ---
        double itemsTotal = 0.0;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found ID: " + itemRequest.getProductId()));

            // Stock Check
            if (product.getQuantity() < itemRequest.getQuantity()) {
                throw new RuntimeException("Not enough stock for: " + product.getName());
            }

            // Reduce Stock
            product.setQuantity(product.getQuantity() - itemRequest.getQuantity());
            productRepository.save(product);

            // Create OrderItem
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPrice(product.getDiscountedPrice());

            orderItems.add(orderItem);

            // Math is simpler with Double
            double lineTotal = orderItem.getPrice() * itemRequest.getQuantity();
            itemsTotal += lineTotal;
        }

        // Final Total
        order.setTotalAmount(itemsTotal + shippingCharge);
        order.setOrderItems(orderItems);

        // --- 3. STATUS SETUP ---
        if (request.getPaymentMethod() == PaymentMethod.COD) {
            // COD Logic
            order.setPaymentStatus(PaymentStatus.PENDING);
            order.setOrderStatus(OrderStatus.CONFIRMED);

        } else {
            order.setPaymentStatus(PaymentStatus.PENDING);
            order.setOrderStatus(OrderStatus.PENDING);
        }

        // --- 4. SAVE & CLEAR CART ---
        Order savedOrder = orderRepository.save(order);
        clearUserCart(user);

        return savedOrder.getId();
    }

    private void clearUserCart(User user) {
        cartRepository.findByUser(user).ifPresent(cart -> {
            cartItemRepository.deleteAllByCartId(cart.getId());
            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }

    @Override
    public OrderDto update(OrderDto orderDto, Long orderId) {

        Order order = orderRepository.findById(orderId).orElseThrow(() ->
                new ResourceNotFound("Order", "id", orderId));

        String formattedTime = dateTimeUtil.convert();

        try {
            if (orderDto.isDelivered()) {
                order.setDelivered(true);
                order.setDeliveredAt(LocalDateTime.now());
            } else {
                order.setDelivered(false);
                order.setDeliveredAt(null);
            }
        } catch (NullPointerException e) {
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
        //response.setContent(content);
        response.setPageNo(orders.getNumber());
        response.setPageSize(orders.getSize());
        response.setTotalPages(orders.getTotalPages());
        response.setTotalElements(orders.getTotalElements());
        response.setLast(orders.isLast());

        return response;
    }

    @Override
    public String delete(Long orderId) {

        Order order = orderRepository.findById(orderId).orElseThrow(() ->
                new ResourceNotFound("Order", "id", orderId));
        orderRepository.delete(order);
        return "your order : " + order.getId() + " is deleted successfully.";
    }

    @Override
    public OrderConfirmationResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + id));
        return mapToOrderResponse(order);
    }

    @Override
    public OrderResponse myOrders(HttpServletRequest servletRequest, int pageNo, int pageSize) {
        User user = tokenUtil.extractUserInfo(servletRequest);
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<Order> ordersPage = orderRepository.findAllByUserOrderByCreatedAtDesc(user, pageable);

        // Map Entity -> DTO
        List<OrderConfirmationResponse> content = ordersPage.getContent().stream()
                .map(this::mapToOrderResponse) // Reuse your existing mapper
                .toList();

        return OrderResponse.builder()
                .content(content)
                .pageNo(ordersPage.getNumber())
                .pageSize(ordersPage.getSize())
                .totalPages(ordersPage.getTotalPages())
                .totalElements(ordersPage.getTotalElements())
                .last(ordersPage.isLast())
                .build();
    }

    private OrderConfirmationResponse mapToOrderResponse(Order order) {
        OrderConfirmationResponse response = new OrderConfirmationResponse();
        response.setId(order.getId());

        // Map User (Safely)
        OrderConfirmationResponse.OrderUserDTO userDto = new OrderConfirmationResponse.OrderUserDTO();
        userDto.setName(order.getName());
        userDto.setEmail(order.getEmail());
        userDto.setPhone(order.getPhoneNumber()); // Use order phone or user phone
        response.setUser(userDto);

        // Map Details
        response.setShippingAddress(order.getShippingAddress());
        response.setCity(order.getCity());
        response.setArea(order.getArea());
        response.setPhoneNumber(order.getPhoneNumber());
        response.setEmail(order.getEmail());
        response.setOrderNote(order.getOrderNote());
        response.setPaymentMethod(order.getPaymentMethod().toString());
        response.setOrderStatus(order.getOrderStatus().toString());
        response.setShippingCost(order.getShippingCost());
        response.setTotalAmount(order.getTotalAmount());
        response.setCreatedAt(order.getCreatedAt());

        // Map Items
        List<OrderConfirmationResponse.OrderItemResponse> items = order.getOrderItems().stream().map(item -> {
            OrderConfirmationResponse.OrderItemResponse itemDto = new OrderConfirmationResponse.OrderItemResponse();
            itemDto.setProductId(item.getProduct().getId());
            itemDto.setProductName(item.getProduct().getName());
            // Handle image safely (check nulls)
            if (item.getProduct().getImageUrls() != null && !item.getProduct().getImageUrls().isEmpty()) {
                itemDto.setProductImageUrl(item.getProduct().getImageUrls().get(0).getImageUrl());
            }
            itemDto.setQuantity(item.getQuantity());
            itemDto.setPrice(item.getPrice());
            itemDto.setTotal(item.getPrice() * item.getQuantity());
            return itemDto;
        }).toList();

        response.setOrderItems(items);
        return response;
    }

    public Order mapToEntity(OrderDto orderDto) {
        return mapper.map(orderDto, Order.class);
    }

    public OrderDto mapToDto(Order order) {
        return mapper.map(order, OrderDto.class);
    }
}

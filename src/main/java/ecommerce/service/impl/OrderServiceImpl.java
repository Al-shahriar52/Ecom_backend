package ecommerce.service.impl;

import ecommerce.dto.OrderDto;
import ecommerce.dto.admin.coupon.CouponValidateRequestDto;
import ecommerce.dto.admin.coupon.CouponValidateResponseDto;
import ecommerce.dto.order.OrderConfirmationResponse;
import ecommerce.dto.order.OrderRequest;
import ecommerce.dto.pageResponse.OrderResponse;
import ecommerce.entity.*;
import ecommerce.enums.InvoiceStatus;
import ecommerce.enums.OrderStatus;
import ecommerce.enums.PaymentMethod;
import ecommerce.enums.PaymentStatus;
import ecommerce.exceptionHandling.BadRequestException;
import ecommerce.exceptionHandling.ResourceNotFound;
import ecommerce.repository.*;
import ecommerce.service.ActivityService;
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
import java.util.*;

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
    private final InvoiceRepository invoiceRepository;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final ActivityService activityService;
    private final SmsService smsService;
    private final CouponService couponService;

    private void clearUserCart(User user) {
        cartRepository.findByUser(user).ifPresent(cart -> {
            cartItemRepository.deleteAllByCartId(cart.getId());
            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }

    @Override
    @Transactional
    public Long placeOrder(HttpServletRequest servletRequest, OrderRequest request) {
        // --- FETCH CURRENT SESSION USER ---
        User currentUser = tokenUtil.extractUserInfo(servletRequest);
        User finalOrderUser = currentUser;

        // --- LOOKUP DB FOR COLLISIONS ---
        Optional<User> userWithEmail = userRepository.findByEmail(request.getEmail());
        Optional<User> userWithPhone = userRepository.findByPhone(request.getPhone());

        boolean emailExistsElsewhere = userWithEmail.isPresent();
        boolean phoneExistsElsewhere = userWithPhone.isPresent();
        boolean isCurrentGuest = (currentUser.getStatus() == null || !currentUser.getStatus());

        if (isCurrentGuest) {
            // CASE 1: Checkout email belongs to an existing account
            if (emailExistsElsewhere) {
                finalOrderUser = userWithEmail.get();
            }
            // CASE 3: Guest session already has an email, but inputs a completely DIFFERENT email
            else if (currentUser.getEmail() != null && !currentUser.getEmail().trim().isEmpty()
                    && !currentUser.getEmail().equalsIgnoreCase(request.getEmail())) {

                User newGuest = new User();
                newGuest.setName(request.getName());
                newGuest.setEmail(request.getEmail());
                newGuest.setPhone(request.getPhone());
                newGuest.setPassword(java.util.UUID.randomUUID().toString());
                newGuest.setStatus(false);

                finalOrderUser = userRepository.save(newGuest);
            }
            // CASE 2: Clean slate guest session
            else {
                currentUser.setName(request.getName());
                currentUser.setEmail(request.getEmail());
                if (!phoneExistsElsewhere) {
                    currentUser.setPhone(request.getPhone());
                }
                if (currentUser.getPassword() == null) {
                    currentUser.setPassword(java.util.UUID.randomUUID().toString());
                }
                currentUser.setStatus(false);
                finalOrderUser = userRepository.save(currentUser);
            }
        } else {
            // CASE 4: Logged-in / Registered User Flow
            if (emailExistsElsewhere && !userWithEmail.get().getId().equals(currentUser.getId())) {
                throw new RuntimeException("This email is already verified with another active account.");
            }
            if (phoneExistsElsewhere && !userWithPhone.get().getId().equals(currentUser.getId())) {
                throw new RuntimeException("This phone number is already verified with another active account.");
            }

            currentUser.setName(request.getName());
            currentUser.setEmail(request.getEmail());
            currentUser.setPhone(request.getPhone());
            finalOrderUser = userRepository.save(currentUser);
        }

        // --- CREATE & POPULATE ORDER ---
        Order order = new Order();
        order.setUser(finalOrderUser);
        order.setGuestUserId(currentUser.getId());
        order.setShippingAddress(request.getShippingAddress());
        order.setCity(request.getCity());
        order.setArea(request.getArea());
        order.setPhoneNumber(request.getPhone());
        order.setEmail(request.getEmail());
        order.setName(request.getName());
        order.setOrderNote(request.getOrderNote());
        order.setPaymentMethod(request.getPaymentMethod());

        // --- 1. CALCULATE SHIPPING ---
        double shippingCharge = request.getCity().trim().equalsIgnoreCase("Dhaka") ? 60.00 : 120.00;
        order.setShippingCost(shippingCharge);

        // --- 2. PROCESS ITEMS & CALCULATE EXACT SUB-TOTALS ---
        double subTotalMrp = 0.0;
        double discountedSubTotal = 0.0;

        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found ID: " + itemRequest.getProductId()));

            if (product.getQuantity() < itemRequest.getQuantity()) {
                throw new RuntimeException("Not enough stock for: " + product.getName());
            }

            // Deduct Stock
            product.setQuantity(product.getQuantity() - itemRequest.getQuantity());
            productRepository.save(product);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPrice(product.getDiscountedPrice());
            orderItems.add(orderItem);

            // Track both MRP and selling price totals for the invoice breakdown
            subTotalMrp += (product.getOriginalPrice() * itemRequest.getQuantity());
            discountedSubTotal += (product.getDiscountedPrice() * itemRequest.getQuantity());
        }

        double totalProductSavings = subTotalMrp - discountedSubTotal;

        // --- 3. APPLY COUPON USING YOUR VALIDATOR ---
        double couponDiscountAmount = 0.0;

        if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {

            // Call your existing method
            CouponValidateResponseDto couponRes = couponService.dryRunCoupon(request.getCouponCode(), servletRequest);

            if (!couponRes.isValid()) {
                throw new RuntimeException("Coupon error: " + couponRes.getMessage());
            }

            couponDiscountAmount = couponRes.getDiscountAmount();
            order.setCouponCode(request.getCouponCode().trim().toUpperCase());
        }

        order.setDiscountAmount(couponDiscountAmount);

        // --- 4. GRAND TOTAL ---
        double finalTotal = Math.max(0, discountedSubTotal - couponDiscountAmount) + shippingCharge;

        order.setTotalAmount(finalTotal);
        order.setOrderItems(orderItems);

        // --- 5. STATUS SETUP ---
        if (request.getPaymentMethod() == ecommerce.enums.PaymentMethod.COD) {
            order.setPaymentStatus(ecommerce.enums.PaymentStatus.PENDING);
            order.setOrderStatus(ecommerce.enums.OrderStatus.CONFIRMED);
        } else {
            order.setPaymentStatus(ecommerce.enums.PaymentStatus.PENDING);
            order.setOrderStatus(ecommerce.enums.OrderStatus.PENDING);
        }

        Order savedOrder = orderRepository.save(order);

        // --- 6. GENERATE DETAILED INVOICE ---
        Invoice invoice = new Invoice();
        invoice.setOrder(savedOrder);
        invoice.setInvoiceNumber("INV-" + (10000 + savedOrder.getId()));

        // --> New Detailed Receipt Fields
        invoice.setSubTotalMrp(subTotalMrp);
        invoice.setProductSavings(totalProductSavings);
        invoice.setDiscountedSubTotal(discountedSubTotal);
        invoice.setCouponCode(order.getCouponCode());
        invoice.setCouponDiscountAmount(couponDiscountAmount);

        // --> Standard Legacy Fields
        invoice.setSubTotal(discountedSubTotal);
        invoice.setShippingAmount(shippingCharge);
        invoice.setDiscountAmount(couponDiscountAmount);
        invoice.setTaxAmount(0.0);
        invoice.setTotalAmount(finalTotal);

        invoice.setIssuedAt(java.time.LocalDateTime.now());
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setDueDate(request.getPaymentMethod() == ecommerce.enums.PaymentMethod.COD ?
                java.time.LocalDateTime.now().plusDays(7) : java.time.LocalDateTime.now().plusHours(24));

        invoiceRepository.save(invoice);

        // --- 7. CLEAR CART & CONFIRM ---
        clearUserCart(currentUser);
        emailService.sendOrderConfirmationEmail(finalOrderUser, savedOrder, invoice);
        smsService.sendOrderConfirmationSms(order.getPhoneNumber(), invoice.getInvoiceNumber(), savedOrder.getTotalAmount());
        activityService.logActivity(finalOrderUser.getId(), "Placed order ID: " + savedOrder.getId() + " with total amount: " + savedOrder.getTotalAmount());

        return savedOrder.getId();
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

        activityService.logActivity(order.getUser().getId(), "Updated order ID: " + order.getId() + " delivery status to: " + orderDto.isDelivered() + " at " + formattedTime);
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

        activityService.logActivity(order.getUser().getId(), "Deleted order ID: " + order.getId() + " at " + dateTimeUtil.convert());
        return "your order : " + order.getId() + " is deleted successfully.";
    }

    @Override
    public OrderConfirmationResponse getOrderById(HttpServletRequest servletRequest, Long id) {
        User user = tokenUtil.extractUserInfo(servletRequest);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + id));

        if (Objects.equals(order.getUser().getId(), user.getId())
                || Objects.equals(order.getGuestUserId(), user.getId())
                || user.getRoles().stream().anyMatch(role -> role.equals(Role.ADMIN))) {
            return mapToOrderResponse(order);
        } else {
            throw new BadRequestException("You are not authorized to view this order.");
        }

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

    public byte[] generateInvoice(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Fetch associated invoice
        Invoice invoice = order.getInvoice();

        return emailService.generatePdfInvoice(order, invoice);
    }

    public Order mapToEntity(OrderDto orderDto) {
        return mapper.map(orderDto, Order.class);
    }

    public OrderDto mapToDto(Order order) {
        return mapper.map(order, OrderDto.class);
    }
}

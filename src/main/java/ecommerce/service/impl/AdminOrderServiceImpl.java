package ecommerce.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import ecommerce.dto.admin.AdminOrderListDTO;
import ecommerce.dto.admin.OrderStatsDTO;
import ecommerce.entity.Delivery;
import ecommerce.entity.Invoice;
import ecommerce.entity.Order;
import ecommerce.enums.InvoiceStatus;
import ecommerce.exceptionHandling.BadRequestException;
import ecommerce.repository.DeliveryRepository;
import ecommerce.repository.InvoiceRepository;
import ecommerce.repository.OrderRepository;
import ecommerce.service.AdminOrderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;
    private final SteadfastService steadfastService;
    private final DeliveryRepository deliveryRepository;

    @Override
    public Page<AdminOrderListDTO> getAdminOrders(
            int page, int size, String search, String method, String paymentStatus,
            String orderStatus, String deliveryStatus, LocalDateTime startDate, LocalDateTime endDate) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Specification<Order> spec = OrderSpecification.getFilteredOrders(
                search, method, paymentStatus, orderStatus, deliveryStatus, startDate, endDate
        );

        Page<Order> orderPage = orderRepository.findAll(spec, pageable);

        // Map Entity to DTO
        return orderPage.map(this::mapToDTO);
    }

    @Override
    public Page<ecommerce.dto.admin.AdminTransactionDto> getFinanceTransactions(
            int page, int size, String search, String method, String paymentStatus,
            String orderStatus, String deliveryStatus, LocalDateTime startDate, LocalDateTime endDate) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Specification<Order> spec = OrderSpecification.getFilteredOrders(
                search, method, paymentStatus, orderStatus, deliveryStatus, startDate, endDate
        );
        Page<Order> orderPage = orderRepository.findAll(spec, pageable);
        return orderPage.map(this::mapToTransactionDto);
    }

    // NOTE: fee rates are hardcoded here for now - the Payment Gateways
    // settings screen in the frontend isn't wired to a real config endpoint
    // yet, so these values need to be kept in sync manually until that
    // module is built.
    private static final java.util.Map<String, Double> GATEWAY_FEE_RATES = java.util.Map.of(
            "BKASH", 1.85, "NAGAD", 1.99, "ROCKET", 1.80, "CARD", 2.75, "COD", 1.20
    );

    private ecommerce.dto.admin.AdminTransactionDto mapToTransactionDto(Order order) {
        ecommerce.dto.admin.AdminTransactionDto dto = new ecommerce.dto.admin.AdminTransactionDto();
        dto.setOrderId(order.getId());
        dto.setInvoice(order.getInvoice() != null ? order.getInvoice().getInvoiceNumber() : null);
        dto.setDate(order.getCreatedAt());
        dto.setCustomer(order.getName());
        dto.setPhone(order.getPhoneNumber());

        String method = order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "COD";
        dto.setGateway(method.toLowerCase());

        double gross = order.getTotalAmount() != null ? order.getTotalAmount() : 0;
        double rate = GATEWAY_FEE_RATES.getOrDefault(method, 0.0);
        dto.setGross(gross);
        dto.setFeeRate(rate);

        String status = deriveStatus(order);
        dto.setStatus(status);

        boolean noValue = "failed".equals(status);
        double fee = noValue ? 0 : (gross * rate / 100.0);
        dto.setFee(fee);
        dto.setNet(noValue ? 0 : gross - fee);

        return dto;
    }

    private String deriveStatus(Order order) {
        String paymentStatus = order.getPaymentStatus() != null ? order.getPaymentStatus().name() : "PENDING";
        String orderStatus = order.getOrderStatus() != null ? order.getOrderStatus().name() : "PENDING";
        String deliveryStatus = order.getDelivery() != null && order.getDelivery().getDeliveryStatus() != null
                ? order.getDelivery().getDeliveryStatus().name() : null;
        boolean isCod = order.getPaymentMethod() == null || "COD".equals(order.getPaymentMethod().name());

        if ("CANCELLED".equals(orderStatus)) return "cancelled";
        if ("FAILED".equals(paymentStatus)) return "failed";
        if ("REFUNDED".equals(paymentStatus)) return "refunded";

        if (isCod) {
            if ("IN_TRANSIT".equals(deliveryStatus)) return "transit";
            if ("DELIVERED".equals(deliveryStatus) && !"PAID".equals(paymentStatus)) return "delivered";
            if ("PAID".equals(paymentStatus)) return "settled";
            return "await";
        }

        return "PAID".equals(paymentStatus) ? "settled" : "await";
    }

    @Override
    public OrderStatsDTO getOrderStats() {
        return OrderStatsDTO.builder()
                .totalOrders(orderRepository.count())
                .pendingPickup(orderRepository.countPendingPickup())
                .inTransit(orderRepository.countByDeliveryStatus(ecommerce.enums.DeliveryStatus.IN_TRANSIT))
                .delivered(orderRepository.countByDeliveryStatus(ecommerce.enums.DeliveryStatus.DELIVERED))
                .cancelled(orderRepository.countByOrderStatus(ecommerce.enums.OrderStatus.CANCELLED))
                .totalBalance(orderRepository.sumTotalBalance())
                .build();
    }

    @Transactional
    public void cancelOrders(List<String> invoices) {
        if (invoices == null || invoices.isEmpty()) {
            throw new IllegalArgumentException("Invoice list cannot be empty");
        }

        // Step 1: Get the exact Order IDs linked to these Invoices
        List<Long> orderIds = invoiceRepository.findOrderIdsByInvoiceNumbers(invoices);

        if (!orderIds.isEmpty()) {
            invoiceRepository.updateInvoiceStatuses(invoices, InvoiceStatus.CANCELLED);
            // Step 2: Bulk update only the Orders at once
            orderRepository.updateOrderStatusesByIds(orderIds, ecommerce.enums.OrderStatus.CANCELLED);
        } else {
            throw new BadRequestException("No orders found for the provided invoice numbers");
        }
    }

    /*@Transactional
    public void requestParcelPickup(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) return;

        List<Order> orders = orderRepository.findAllById(orderIds);
        List<Invoice> invoices = invoiceRepository.findByOrder_IdIn(orderIds);

        Map<Long, Invoice> invoiceMap = new HashMap<>();
        for (Invoice inv : invoices) {
            invoiceMap.put(inv.getOrder().getId(), inv);
        }

        // Filter out orders that are already shipped to prevent duplicate deliveries
        List<Order> validOrdersForPickup = orders.stream()
                .filter(o -> o.getOrderStatus() != ecommerce.enums.OrderStatus.SHIPPED)
                .toList();

        if (validOrdersForPickup.isEmpty()) {
            throw new RuntimeException("No valid orders available for pickup.");
        }

        // 1. Call the Steadfast Bulk API
        JsonNode responseNode = steadfastService.sendBulkPickupRequest(validOrdersForPickup, invoiceMap);

        if (responseNode != null) {
            // Safely extract the data array
            JsonNode dataArray = null;
            if (responseNode.isArray()) {
                dataArray = responseNode;
            } else if (responseNode.has("data") && responseNode.get("data").isArray()) {
                dataArray = responseNode.get("data");
            }

            if (dataArray != null) {
                List<Delivery> deliveriesToSave = new ArrayList<>();
                List<Order> ordersToUpdate = new ArrayList<>();

                for (JsonNode item : dataArray) {
                    if (item.has("status") && "success".equalsIgnoreCase(item.get("status").asText())) {

                        String returnedInvoiceNumber = item.get("invoice").asText();
                        String consignmentId = item.get("consignment_id").asText();
                        String trackingCode = item.get("tracking_code").asText();

                        // Find the matching order
                        validOrdersForPickup.stream()
                                .filter(o -> invoiceMap.get(o.getId()).getInvoiceNumber().equals(returnedInvoiceNumber))
                                .findFirst()
                                .ifPresent(o -> {

                                    // --- A. UPDATE THE ORDER ---
                                    o.setOrderStatus(ecommerce.enums.OrderStatus.SHIPPED);
                                    ordersToUpdate.add(o);

                                    // --- B. CREATE THE DELIVERY RECORD ---
                                    Delivery delivery = new Delivery();
                                    delivery.setOrder(o);
                                    delivery.setCourierName("Steadfast");
                                    delivery.setConsignmentId(consignmentId);
                                    delivery.setTrackingCode(trackingCode);
                                    delivery.setRequestedAt(LocalDateTime.now());
                                    delivery.setDeliveryStatus(ecommerce.enums.DeliveryStatus.PENDING);

                                    // Determine COD amount based on Payment Method
                                    Invoice inv = invoiceMap.get(o.getId());
                                    double codAmount = o.getPaymentMethod() == ecommerce.enums.PaymentMethod.COD ? inv.getTotalAmount() : 0.0;
                                    delivery.setCodAmount(codAmount);

                                    // You can set a default delivery type based on your business logic
                                    delivery.setDeliveryType("Home Delivery");

                                    deliveriesToSave.add(delivery);
                                });
                    } else {
                        System.err.println("Steadfast failed for invoice: " + item.get("invoice").asText());
                    }
                }

                // Save everything to the database in two efficient bulk batches
                deliveryRepository.saveAll(deliveriesToSave);
                orderRepository.saveAll(ordersToUpdate);
            }
        }
    }*/

    @Transactional
    public void requestParcelPickup(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) return;

        List<Order> orders = orderRepository.findAllById(orderIds);
        List<Invoice> invoices = invoiceRepository.findByOrder_IdIn(orderIds);

        Map<Long, Invoice> invoiceMap = new HashMap<>();
        for (Invoice inv : invoices) {
            invoiceMap.put(inv.getOrder().getId(), inv);
        }

        // Filter out orders that are already shipped to prevent duplicate deliveries
        List<Order> validOrdersForPickup = orders.stream()
                .filter(o -> o.getOrderStatus() != ecommerce.enums.OrderStatus.SHIPPED)
                .toList();

        if (validOrdersForPickup.isEmpty()) {
            throw new RuntimeException("No valid orders available for pickup.");
        }

        List<Delivery> deliveriesToSave = new ArrayList<>();
        List<Order> ordersToUpdate = new ArrayList<>();

        // 1. ROUTING LOGIC: Single vs Bulk
        if (validOrdersForPickup.size() == 1) {

            // --- SINGLE ORDER ---
            Order singleOrder = validOrdersForPickup.get(0);
            Invoice singleInvoice = invoiceMap.get(singleOrder.getId());

            // Note: Make sure steadfastService has this method implemented
            JsonNode singleResponseNode = steadfastService.sendSinglePickupRequest(singleOrder, singleInvoice);
            processResponseData(singleResponseNode, validOrdersForPickup, invoiceMap, deliveriesToSave, ordersToUpdate);

        } else {

            // --- BULK ORDERS ---
            try {
                JsonNode bulkResponseNode = steadfastService.sendBulkPickupRequest(validOrdersForPickup, invoiceMap);

                // If the response is null or indicates a top-level error, throw exception to trigger fallback
                if (bulkResponseNode == null || (bulkResponseNode.has("status") && bulkResponseNode.get("status").asInt() >= 400)) {
                    throw new RuntimeException("Bulk API returned an error or null response.");
                }

                processResponseData(bulkResponseNode, validOrdersForPickup, invoiceMap, deliveriesToSave, ordersToUpdate);

            } catch (Exception e) {
                System.err.println("Bulk API failed. Falling back to single API loop. Error: " + e.getMessage());

                // --- FALLBACK LOOP ---
                for (Order order : validOrdersForPickup) {
                    try {
                        Invoice inv = invoiceMap.get(order.getId());
                        JsonNode fallbackResponse = steadfastService.sendSinglePickupRequest(order, inv);

                        // Process each one individually
                        processResponseData(fallbackResponse, List.of(order), invoiceMap, deliveriesToSave, ordersToUpdate);
                    } catch (Exception ex) {
                        System.err.println("Fallback failed for invoice " + invoiceMap.get(order.getId()).getInvoiceNumber() + ": " + ex.getMessage());
                    }
                }
            }
        }

        // 2. SAVE TO DATABASE
        // Save everything to the database in two efficient bulk batches
        if (!deliveriesToSave.isEmpty()) {
            deliveryRepository.saveAll(deliveriesToSave);
            orderRepository.saveAll(ordersToUpdate);
        }
    }

    /**
     * Helper method to parse the Steadfast JSON response and map it to Delivery and Order entities.
     * This handles both Single API responses (single object) and Bulk API responses (arrays).
     */
    private void processResponseData(
            JsonNode responseNode,
            List<Order> validOrdersForPickup,
            Map<Long, Invoice> invoiceMap,
            List<Delivery> deliveriesToSave,
            List<Order> ordersToUpdate) {

        if (responseNode == null) return;

        List<JsonNode> dataItems = new ArrayList<>();

        // Safely extract the data whether it's an array (Bulk) or a single object wrapped in "consignment" (Single)
        if (responseNode.isArray()) {
            responseNode.forEach(dataItems::add);
        } else if (responseNode.has("data") && responseNode.get("data").isArray()) {
            responseNode.get("data").forEach(dataItems::add);
        } else if (responseNode.has("consignment")) {
            // Common structure for Steadfast single order response
            dataItems.add(responseNode.get("consignment"));
        } else {
            // Fallback for direct object response
            dataItems.add(responseNode);
        }

        for (JsonNode item : dataItems) {
            // Checking for consignment_id is a more reliable success check across both Single and Bulk endpoints
            if (item.has("consignment_id") && item.has("tracking_code")) {

                String returnedInvoiceNumber = item.has("invoice") ? item.get("invoice").asText() : null;
                String consignmentId = item.get("consignment_id").asText();
                String trackingCode = item.get("tracking_code").asText();

                // Find the matching order
                validOrdersForPickup.stream()
                        .filter(o -> {
                            // If Steadfast didn't return the invoice number (happens occasionally on single API),
                            // and we only sent one order, just match it automatically. Otherwise, match by invoice.
                            if (returnedInvoiceNumber == null && validOrdersForPickup.size() == 1) return true;
                            return invoiceMap.get(o.getId()).getInvoiceNumber().equals(returnedInvoiceNumber);
                        })
                        .findFirst()
                        .ifPresent(o -> {

                            // --- A. UPDATE THE ORDER ---
                            o.setOrderStatus(ecommerce.enums.OrderStatus.SHIPPED);
                            ordersToUpdate.add(o);

                            // --- B. CREATE THE DELIVERY RECORD ---
                            Delivery delivery = new Delivery();
                            delivery.setOrder(o);
                            delivery.setCourierName("Steadfast");
                            delivery.setConsignmentId(consignmentId);
                            delivery.setTrackingCode(trackingCode);
                            delivery.setRequestedAt(LocalDateTime.now());
                            delivery.setDeliveryStatus(ecommerce.enums.DeliveryStatus.PENDING);

                            // Determine COD amount based on Payment Method
                            Invoice inv = invoiceMap.get(o.getId());
                            double codAmount = o.getPaymentMethod() == ecommerce.enums.PaymentMethod.COD ? inv.getTotalAmount() : 0.0;
                            delivery.setCodAmount(codAmount);

                            // You can set a default delivery type based on your business logic
                            delivery.setDeliveryType("Home Delivery");

                            deliveriesToSave.add(delivery);
                        });
            } else {
                String errorInv = item.has("invoice") ? item.get("invoice").asText() : "Unknown";
                System.err.println("Steadfast failed or missing data for invoice: " + errorInv);
            }
        }
    }

    @Override
    public ecommerce.dto.admin.AdminOrderDetailDTO getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BadRequestException("Order not found with id: " + orderId));
        return mapToDetailDTO(order);
    }

    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, ecommerce.dto.admin.UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BadRequestException("Order not found with id: " + orderId));

        // --- Status ---
        if (request.getOrderStatus() != null && !request.getOrderStatus().isBlank()) {
            try {
                order.setOrderStatus(ecommerce.enums.OrderStatus.valueOf(request.getOrderStatus()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid order status: " + request.getOrderStatus());
            }
        }

        if (request.getDeliveryStatus() != null && !request.getDeliveryStatus().isBlank()) {
            if (order.getDelivery() == null) {
                throw new BadRequestException("This order has no delivery record yet - request a pickup first.");
            }
            try {
                order.getDelivery().setDeliveryStatus(ecommerce.enums.DeliveryStatus.valueOf(request.getDeliveryStatus()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid delivery status: " + request.getDeliveryStatus());
            }
        }

        // --- Contact ---
        if (request.getCustomerName() != null) order.setName(request.getCustomerName());
        if (request.getPhone() != null) order.setPhoneNumber(request.getPhone());
        if (request.getEmail() != null) order.setEmail(request.getEmail());
        if (request.getShippingAddress() != null) order.setShippingAddress(request.getShippingAddress());
        if (request.getCity() != null) order.setCity(request.getCity());
        if (request.getArea() != null) order.setArea(request.getArea());

        // --- Payment ---
        if (request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank()) {
            try {
                order.setPaymentMethod(ecommerce.enums.PaymentMethod.valueOf(request.getPaymentMethod()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid payment method: " + request.getPaymentMethod());
            }
        }
        if (request.getPaymentStatus() != null && !request.getPaymentStatus().isBlank()) {
            try {
                order.setPaymentStatus(ecommerce.enums.PaymentStatus.valueOf(request.getPaymentStatus()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid payment status: " + request.getPaymentStatus());
            }
        }

        // --- Financials ---
        if (request.getShippingCost() != null) order.setShippingCost(request.getShippingCost());
        if (request.getTotalAmount() != null) order.setTotalAmount(request.getTotalAmount());

        // --- Courier (manual correction only - normally system-managed) ---
        if (request.getCid() != null && order.getDelivery() != null) order.getDelivery().setConsignmentId(request.getCid());
        if (request.getTrackingCode() != null && order.getDelivery() != null) order.getDelivery().setTrackingCode(request.getTrackingCode());

        // --- Misc ---
        if (request.getOrderNote() != null) order.setOrderNote(request.getOrderNote());

        orderRepository.save(order);
    }

    private ecommerce.dto.admin.AdminOrderDetailDTO mapToDetailDTO(Order order) {
        ecommerce.dto.admin.AdminOrderDetailDTO dto = new ecommerce.dto.admin.AdminOrderDetailDTO();

        dto.setId(order.getId());
        dto.setDate(order.getCreatedAt());
        dto.setCustomer(order.getName());
        dto.setPhone(order.getPhoneNumber());
        dto.setEmail(order.getEmail());
        dto.setAddress(order.getCity() + ", " + order.getArea() + ", " + order.getShippingAddress());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setShippingCost(order.getShippingCost());
        dto.setOrderNote(order.getOrderNote());
        dto.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "-");
        dto.setPaymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : "-");
        dto.setOrderStatus(order.getOrderStatus() != null ? order.getOrderStatus().name() : "-");

        if (order.getInvoice() != null) {
            dto.setInvoice(order.getInvoice().getInvoiceNumber());
        }

        if (order.getDelivery() != null) {
            dto.setDeliveryStatus(order.getDelivery().getDeliveryStatus() != null ? order.getDelivery().getDeliveryStatus().name() : "-");
            dto.setCid(order.getDelivery().getConsignmentId());
            dto.setTrackingCode(order.getDelivery().getTrackingCode());
        }

        List<ecommerce.dto.admin.AdminOrderItemDTO> items = order.getOrderItems().stream().map(item -> {
            ecommerce.dto.admin.AdminOrderItemDTO i = new ecommerce.dto.admin.AdminOrderItemDTO();
            i.setQuantity(item.getQuantity());
            i.setPrice(item.getPrice());
            i.setSubtotal(item.getQuantity() * item.getPrice());
            if (item.getProduct() != null) {
                i.setProductId(item.getProduct().getId());
                i.setProductName(item.getProduct().getName());
                if (item.getProduct().getImageUrls() != null && !item.getProduct().getImageUrls().isEmpty()) {
                    i.setProductImage(item.getProduct().getImageUrls().get(0).getImageUrl());
                }
            }
            return i;
        }).collect(java.util.stream.Collectors.toList());
        dto.setItems(items);

        return dto;
    }

    private AdminOrderListDTO mapToDTO(Order order) {
        AdminOrderListDTO dto = new AdminOrderListDTO();

        dto.setDate(order.getCreatedAt());
        dto.setCustomer(order.getName());
        dto.setPhone(order.getPhoneNumber());
        dto.setEmail(order.getEmail());
        dto.setAddress(order.getCity() + ", " + order.getArea() + ", " + order.getShippingAddress());
        dto.setId(order.getId());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "-");
        dto.setPaymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : "-");
        dto.setOrderStatus(order.getOrderStatus() != null ? order.getOrderStatus().name() : "-");

        if (order.getInvoice() != null) {
            dto.setInvoice(order.getInvoice().getInvoiceNumber());
        }

        if (order.getDelivery() != null) {
            dto.setDeliveryStatus(order.getDelivery().getDeliveryStatus() != null ? order.getDelivery().getDeliveryStatus().name() : "-");
            dto.setCid(order.getDelivery().getConsignmentId());
        }

        return dto;
    }
}
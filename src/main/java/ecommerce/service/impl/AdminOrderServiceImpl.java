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
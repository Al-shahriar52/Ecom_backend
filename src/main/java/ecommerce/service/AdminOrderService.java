package ecommerce.service;

import ecommerce.dto.admin.*;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminOrderService {
    Page<AdminOrderListDTO> getAdminOrders(
            int page, int size, String search, String method, String paymentStatus,
            String orderStatus, String deliveryStatus, LocalDateTime startDate, LocalDateTime endDate);

    OrderStatsDTO getOrderStats();
    void cancelOrders(List<String> invoices);
    void requestParcelPickup(List<Long> orderIds);

    /** providerKey null = default courier. */
    void requestParcelPickup(List<Long> orderIds, String providerKey);

    /** codOverrides: orderId -> amount to collect, replacing the invoice total for that parcel (may be null). */
    void requestParcelPickup(List<Long> orderIds, String providerKey, java.util.Map<Long, Double> codOverrides);

    AdminOrderDetailDTO getOrderById(Long orderId);
    void updateOrderStatus(Long orderId, UpdateOrderStatusRequest request);

    Page<AdminTransactionDto> getFinanceTransactions(
            int page, int size, String search, String method, String paymentStatus,
            String orderStatus, String deliveryStatus, LocalDateTime startDate, LocalDateTime endDate);
}
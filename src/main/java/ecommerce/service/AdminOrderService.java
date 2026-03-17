package ecommerce.service;

import ecommerce.dto.admin.AdminOrderListDTO;
import ecommerce.dto.admin.OrderStatsDTO;
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
}

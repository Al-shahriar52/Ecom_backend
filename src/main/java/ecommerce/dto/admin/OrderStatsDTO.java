package ecommerce.dto.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderStatsDTO {
    private long totalOrders;
    private long pendingPickup;
    private long inTransit;
    private long delivered;
    private long cancelled;
    private Double totalBalance;
}
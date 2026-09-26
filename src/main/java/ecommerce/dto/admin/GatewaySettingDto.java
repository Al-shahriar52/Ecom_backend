package ecommerce.dto.admin;

import lombok.Data;

@Data
public class GatewaySettingDto {
    private String paymentMethod;
    private double feeRatePct;
    private String settlementCycle;
    private boolean connected;
    private double collectedThisMonth;
    private long orderCount;
}

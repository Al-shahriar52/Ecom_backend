package ecommerce.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Real online-payment settlement figures, calculated from orders and your saved gateway fee rates. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementsDto {
    private String start;
    private String end;
    private double receivedGross;
    private double expectedFees;
    private double expectedNet;
    private double awaitingAmount;
    private long awaitingCount;
    private List<Gateway> gateways;
    private List<Payment> payments;
    private long paymentsTotal;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Gateway {
        private String method;
        private double feeRatePct;
        private long paidCount;
        private double paidGross;
        private double expectedFee;
        private double expectedNet;
        private long awaitingCount;
        private double awaitingAmount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payment {
        private Long orderId;
        private String invoice;
        private String customer;
        private String method;
        private double gross;
        private double fee;
        private double net;
        private String paidAt;
    }
}

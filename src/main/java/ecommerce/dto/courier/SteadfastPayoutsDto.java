package ecommerce.dto.courier;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SteadfastPayoutsDto {
    private Double balance;          // what Steadfast owes us right now (null if the call failed)
    private String balanceError;
    private List<Payout> payouts;
    private String payoutsError;
    private int page;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Payout {
        private String paymentId;
        private double amount;
        private String method;
        private double charges;
        private String status;
        private String createdAt;
        private String paidAt;
    }
}

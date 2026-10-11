package ecommerce.dto.courier;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayoutReconciliationDto {
    private String paymentId;
    private int parcelCount;
    private int matchedCount;
    private double settledTotal;     // sum of the amounts Steadfast lists per parcel
    private double expectedTotal;    // sum of what our invoices say for the matched parcels
    private List<Line> lines;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Line {
        private String invoice;
        private String consignmentId;
        private String courierStatus;
        private Double settledAmount;   // from Steadfast, null if the field wasn't found
        private Long orderId;           // null = no order of ours has this invoice
        private Double invoiceTotal;
        private Double difference;      // settled - invoice (delivery charge and 1% fee explain small gaps)
        private String ourDeliveryStatus;
    }
}

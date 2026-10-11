package ecommerce.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Per-courier cash-on-delivery position. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourierCodStatDto {
    private String courier;
    private long totalParcels;
    private double holdingAmount;       // out for delivery
    private long holdingCount;
    private double unpaidAmount;        // delivered, courier hasn't paid us yet
    private long unpaidCount;
    private double returnedAmount;
    private long returnedCount;
    private double returnRatePct;
}

package ecommerce.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CodSummaryDto {
    private double outForDeliveryAmount;
    private long outForDeliveryCount;
    private double deliveredUnpaidAmount;
    private long deliveredUnpaidCount;
    private double returnedAmount;
    private long returnedCount;
    private double returnRatePct;
    private List<CourierMixItemDto> courierMix;
    private List<AdminCodParcelDto> parcels;
}

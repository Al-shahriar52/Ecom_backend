package ecommerce.dto.courier;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SyncTargetDto {
    private Long deliveryId;
    private String courierName;
    private String consignmentId;
    private String invoiceNumber;
}

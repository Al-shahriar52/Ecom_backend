package ecommerce.dto.courier;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AwaitingPickupDto {
    private Long orderId;
    private String invoiceNumber;
    private String consignmentId;
    private String customerName;
    private String phone;
    private String city;
    private Double codAmount;
    private LocalDateTime requestedAt;
}

package ecommerce.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GatewayMixItemDto {
    private String gateway;
    private double amount;
    private long count;
}

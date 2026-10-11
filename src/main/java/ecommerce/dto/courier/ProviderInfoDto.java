package ecommerce.dto.courier;

import ecommerce.courier.CourierCapabilities;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProviderInfoDto {
    private String provider;
    private String displayName;
    private boolean enabled;
    private boolean defaultProvider;
    private boolean configured;
    private boolean sandbox;
    private CourierCapabilities capabilities;
}

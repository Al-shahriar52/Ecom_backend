package ecommerce.dto.courier;

import lombok.Data;

@Data
public class UpdateProviderRequest {
    private Boolean enabled;
    private Boolean defaultProvider;
}

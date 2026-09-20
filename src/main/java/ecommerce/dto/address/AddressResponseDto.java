package ecommerce.dto.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import ecommerce.entity.AddressType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddressResponseDto {
    private Long id;
    private AddressType addressType;
    private String city;
    private String area;
    private String address;
    @JsonProperty("isDefault")
    private boolean isDefault;
}

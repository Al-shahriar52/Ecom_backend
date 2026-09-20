package ecommerce.dto.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import ecommerce.entity.AddressType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddressRequestDto {

    @NotNull
    private AddressType addressType;
    @NotNull
    private String city;
    @NotNull
    private String area;
    @NotNull
    private String address;
    private Long id;
    @JsonProperty("isDefault")
    private boolean isDefault;
}

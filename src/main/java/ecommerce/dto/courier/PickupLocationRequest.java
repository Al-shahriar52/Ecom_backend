package ecommerce.dto.courier;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PickupLocationRequest {
    @NotBlank @Size(max = 80)
    private String label;
    @NotNull
    private Long steadfastAddressId;
    @NotNull
    private Long policeStationId;
    private String policeStationName;
    @NotBlank @Size(max = 255)
    private String address;
    @NotBlank
    private String contactNumber;
    private boolean primaryLocation;
}

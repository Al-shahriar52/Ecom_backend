package ecommerce.dto.address;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddressRequestDto {

    @NotNull
    private String addressType;
    @NotNull
    private String city;
    @NotNull
    private String area;
    @NotNull
    private String address;
    private Long id;
}

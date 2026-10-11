package ecommerce.dto.courier;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreatePathaoStoreRequest {
    @NotBlank @Size(min = 3, max = 50)
    private String name;
    @NotBlank @Size(min = 3, max = 50)
    private String contactName;
    @NotBlank @Pattern(regexp = "^01\\d{9}$", message = "Contact number must be 11 digits starting with 01")
    private String contactNumber;
    @Pattern(regexp = "^(01\\d{9})?$", message = "Secondary contact must be 11 digits")
    private String secondaryContact;
    @Pattern(regexp = "^(01\\d{9})?$", message = "OTP number must be 11 digits")
    private String otpNumber;
    @NotBlank @Size(min = 15, max = 120)
    private String address;
    @NotNull private Long cityId;
    @NotNull private Long zoneId;
    @NotNull private Long areaId;
}

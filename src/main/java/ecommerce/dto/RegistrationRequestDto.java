package ecommerce.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistrationRequestDto {

    @NotNull
    private String name;

    @NotNull
    private String emailOrPhone;

    @NotNull
    @Size(min = 8, message = "password should have at least 8 character")
    @Pattern(regexp = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,}$",
            message = "At least one uppercase letter, one lowercase letter, one number and one special character")
    private String password;
}

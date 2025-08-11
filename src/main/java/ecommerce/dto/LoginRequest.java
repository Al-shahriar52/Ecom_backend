package ecommerce.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoginRequest {

    @NotNull
    private String emailOrPhone;
    @NotNull
    private String password;
}

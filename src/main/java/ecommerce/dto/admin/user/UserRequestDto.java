package ecommerce.dto.admin.user;

import ecommerce.entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserRequestDto {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @Pattern(
        regexp = "^$|^(?:\\+?88)?01[3-9]\\d{8}$", 
        message = "Please enter a valid Bangladeshi phone number"
    )
    private String phone;

    private String role;

    private Gender gender;
}
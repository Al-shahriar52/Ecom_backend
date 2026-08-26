package ecommerce.dto;

import ecommerce.entity.AccountState;
import ecommerce.entity.Gender;
import ecommerce.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private Gender gender;
    private String dob;
    private Set<Role> roles;
    private AccountState accountState;
    private String createdAt;
    private Integer orders;
    private String avatarVariant;
}

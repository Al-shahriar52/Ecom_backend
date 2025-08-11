package ecommerce.dto;

import ecommerce.entity.Role;
import lombok.Data;

import java.util.Set;

@Data
public class UserDto {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String password;
    private Set<Role> roles;
    private String createdAt;
}

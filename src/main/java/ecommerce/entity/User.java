package ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails; // <-- Import UserDetails

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class User implements UserDetails { // <-- Implement the interface

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @Column(unique = true)
    @Email
    private String email;

    @Column(unique = true)
    @Pattern(regexp = "(^([+]{1}[8]{2}|0088)?(01){1}[3-9]{1}\\d{8})$", message = "Phone number not valid")
    private String phone;

    @Size(min = 8, message = "password should have at least 8 character")
    @Pattern(regexp = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,}$",
            message = "At least one uppercase letter, one lowercase letter, one number and one special character")
    @NotNull
    private String password;

    @Enumerated(EnumType.STRING)
    @ElementCollection(fetch = FetchType.EAGER) // EAGER fetch is often needed for roles during authentication
    private Set<Role> roles = new HashSet<>();

    @CreationTimestamp
    private String createdAt;

    private Boolean status = true;


    // ========= UserDetails Method Implementations =========

    /**
     * Converts the Set of Role enums into a collection of GrantedAuthority objects.
     * This is crucial for role-based authorization.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .collect(Collectors.toList());
    }

    /**
     * Returns the user's email as the username. Spring Security will use this for identification.
     */
    @Override
    public String getUsername() {
        return email != null ? email : phone;
    }

    /**
     * Indicates whether the user's account has expired.
     * Returning true means the account is always valid.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is locked or not.
     * Returning true means the user is never locked.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indicates whether the user's credentials (password) has expired.
     * Returning true means the credentials are always valid.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is enabled or disabled.
     * This implementation maps directly to your 'status' field.
     */
    @Override
    public boolean isEnabled() {
        return this.status;
    }
}
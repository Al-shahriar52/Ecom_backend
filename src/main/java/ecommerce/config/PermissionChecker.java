package ecommerce.config;

import ecommerce.service.impl.RoleMatrixService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Used in @PreAuthorize: {@code @PreAuthorize("@perm.has(authentication, 13)")}.
 * True when the signed-in user's role has that permission in the Roles & Permissions matrix (admin always does).
 */
@Component("perm")
@RequiredArgsConstructor
public class PermissionChecker {

    private final RoleMatrixService roleMatrixService;

    public boolean has(Authentication auth, int index) {
        if (auth == null || !auth.isAuthenticated()) return false;
        List<String> roles = new ArrayList<>();
        for (GrantedAuthority ga : auth.getAuthorities()) roles.add(ga.getAuthority());
        return roleMatrixService.hasPermission(roles, index);
    }
}

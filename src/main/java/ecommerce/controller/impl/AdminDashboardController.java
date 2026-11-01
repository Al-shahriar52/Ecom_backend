package ecommerce.controller.impl;

import ecommerce.dto.GenericResponseDto;
import ecommerce.service.impl.AdminDashboardService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/** GET /api/v1/admin/dashboard?days=7|30|90 - needs "View dashboard"; each block also needs its own permission. */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminDashboardController {

    private final AdminDashboardService service;

    @GetMapping
    @PreAuthorize("@perm.has(authentication, 0)")
    public ResponseEntity<?> dashboard(@RequestParam(defaultValue = "30") int days, Authentication auth) {
        List<String> roles = new ArrayList<>();
        for (GrantedAuthority ga : auth.getAuthorities()) roles.add(ga.getAuthority());
        return ResponseEntity.ok(GenericResponseDto.success("Dashboard", service.build(roles, days), HttpStatus.OK.value()));
    }
}

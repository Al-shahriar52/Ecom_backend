package ecommerce.controller.impl;

import ecommerce.controller.AuthController;
import ecommerce.dto.*;
import ecommerce.service.AuthService;
import ecommerce.service.impl.AuthServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthControllerImpl implements AuthController {

    private final AuthServiceImpl authService;
    @PostMapping("/register")
    @Override
    public ResponseEntity<?> register(@RequestBody @Valid RegistrationRequestDto requestDto) {
        RegistrationResponseDto register = authService.register(requestDto);
        return new ResponseEntity<>(GenericResponseDto.success("User registration successfully ", register, HttpStatus.OK.value()), HttpStatus.OK);
    }

    @PostMapping("/login")
    @Override
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest loginRequest, HttpServletResponse servletResponse) {

        LoginResponse response = authService.login(loginRequest, servletResponse);

        return new ResponseEntity<>(GenericResponseDto.success("Login successfully", response, HttpStatus.OK.value()), HttpStatus.OK);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if(authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>(GenericResponseDto.error("UnAuthorized", "Not Authenticated", HttpStatus.UNAUTHORIZED.value()), HttpStatus.UNAUTHORIZED);
        }

        String userRole = authentication.getAuthorities().iterator().next().getAuthority();
        LoginResponse authData =LoginResponse.builder()
                .name(authentication.getName())
                .role(userRole)
                .build();

        return new ResponseEntity<>(GenericResponseDto.success("Fetched user info successfully", authData, HttpStatus.OK.value()), HttpStatus.OK);
    }
}

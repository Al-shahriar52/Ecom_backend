package ecommerce.controller.impl;

import ecommerce.controller.AuthController;
import ecommerce.dto.*;
import ecommerce.service.AuthService;
import ecommerce.service.impl.AuthServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest loginRequest) {

        LoginResponse response = authService.login(loginRequest);
        return new ResponseEntity<>(GenericResponseDto.success("Login successfully", response, HttpStatus.OK.value()), HttpStatus.OK);
    }
}

package ecommerce.controller;

import ecommerce.dto.LoginRequest;
import ecommerce.dto.RegistrationRequestDto;
import ecommerce.dto.RegistrationResponseDto;
import ecommerce.dto.VerifyOtpRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

public interface AuthController {
    ResponseEntity<?> register(RegistrationRequestDto requestDto);
    ResponseEntity<?> login(LoginRequest loginRequest, HttpServletResponse servletResponse);
    ResponseEntity<?> verifyOtp(VerifyOtpRequest request);
}

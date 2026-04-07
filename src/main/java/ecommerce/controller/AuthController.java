package ecommerce.controller;

import ecommerce.dto.LoginRequest;
import ecommerce.dto.RegistrationRequestDto;
import ecommerce.dto.VerifyOtpRequest;
import ecommerce.dto.auth.ForgotPasswordRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

public interface AuthController {
    ResponseEntity<?> register(RegistrationRequestDto requestDto);
    ResponseEntity<?> login(LoginRequest loginRequest, HttpServletResponse servletResponse);
    ResponseEntity<?> verifyOtp(VerifyOtpRequest request);
    ResponseEntity<?> forgotPassword(ForgotPasswordRequest request);
}

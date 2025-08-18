package ecommerce.service;

import ecommerce.dto.LoginRequest;
import ecommerce.dto.LoginResponse;
import ecommerce.dto.RegistrationRequestDto;
import ecommerce.dto.RegistrationResponseDto;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    RegistrationResponseDto register(RegistrationRequestDto requestDto);
    LoginResponse login(LoginRequest authenticationRequest);
    LoginResponse refreshToken(HttpServletRequest request);
}

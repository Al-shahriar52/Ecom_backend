package ecommerce.service;

import ecommerce.dto.LoginRequest;
import ecommerce.dto.LoginResponse;
import ecommerce.dto.RegistrationRequestDto;
import ecommerce.dto.RegistrationResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    RegistrationResponseDto register(RegistrationRequestDto requestDto);
    LoginResponse login(LoginRequest authenticationRequest, HttpServletResponse servletResponse);
    LoginResponse refreshToken(HttpServletRequest request, HttpServletResponse servletResponse);
}

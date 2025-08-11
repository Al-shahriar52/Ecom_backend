package ecommerce.service;

import ecommerce.dto.LoginRequest;
import ecommerce.dto.LoginResponse;
import ecommerce.dto.RegistrationRequestDto;
import ecommerce.dto.RegistrationResponseDto;

public interface AuthService {

    RegistrationResponseDto register(RegistrationRequestDto requestDto);
    LoginResponse login(LoginRequest authenticationRequest);
}

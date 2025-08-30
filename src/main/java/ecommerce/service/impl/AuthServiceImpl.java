package ecommerce.service.impl;

import ecommerce.config.JwtService;
import ecommerce.dto.LoginRequest;
import ecommerce.dto.LoginResponse;
import ecommerce.dto.RegistrationRequestDto;
import ecommerce.dto.RegistrationResponseDto;
import ecommerce.entity.Role;
import ecommerce.entity.User;
import ecommerce.exceptionHandling.BadRequestException;
import ecommerce.exceptionHandling.ForbiddenException;
import ecommerce.exceptionHandling.UnauthorizedException;
import ecommerce.repository.UserRepository;
import ecommerce.service.AuthService;
import ecommerce.utils.ContactValidator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    @Override
    public RegistrationResponseDto register(RegistrationRequestDto requestDto) {

        ContactValidator.ContactType type = ContactValidator.identifyContactType(requestDto.getEmailOrPhone());

        Optional<User> existingUser = userRepository.findByEmailOrPhoneAndStatusIsTrue(requestDto.getEmailOrPhone(), requestDto.getEmailOrPhone());
        if (existingUser.isPresent()) {
            throw new BadRequestException("This email or phone number is already exist");
        }
        User user = new User();
        user.setName(requestDto.getName());
        user.setPassword(passwordEncoder.encode(requestDto.getPassword()));
        user.setRoles(Set.of(Role.ADMIN));


        switch (type) {
            case EMAIL:
                // Handle email registration
                user.setEmail(requestDto.getEmailOrPhone());
                break;

            case PHONE:
                // Handle phone registration
                user.setPhone(requestDto.getEmailOrPhone());
                break;

            case INVALID:
                throw new BadRequestException("Invalid email or phone number format");
        }
        userRepository.save(user);
        return new RegistrationResponseDto(requestDto.getName());
    }

    public LoginResponse login(LoginRequest authenticationRequest) {

        var user = userRepository.findByEmailOrPhoneAndStatusIsTrue(authenticationRequest.getEmailOrPhone(), authenticationRequest.getEmailOrPhone()).orElseThrow(()->
                new BadRequestException("No account found. Please register first."));

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authenticationRequest.getEmailOrPhone(),
                        authenticationRequest.getPassword()
                )
        );

        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        return LoginResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer").build();
    }

    @Override
    public LoginResponse refreshToken(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");
        final String refreshToken;
        final String username;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Authorization is required");
        }

        refreshToken = authHeader.substring(7);
        username = jwtService.extractUsername(refreshToken);

        if (username != null) {
            var userDetails = this.userRepository.findByEmailOrPhoneAndStatusIsTrue(username, username)
                    .orElseThrow();

            if (jwtService.isTokenValid(refreshToken, userDetails)) {
                var accessToken = jwtService.generateToken(userDetails);
                return LoginResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken) // Return the original refresh token
                        .build();
            }else {
                throw new ForbiddenException("Invalid token");
            }
        }else {
            throw new BadRequestException("Invalid user request");
        }
    }

}

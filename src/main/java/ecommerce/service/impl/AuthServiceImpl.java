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
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
        user.setRoles(Set.of(Role.USER));


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

    public LoginResponse login(LoginRequest authenticationRequest, HttpServletResponse servletResponse) {

        var user = userRepository.findByEmailOrPhoneAndStatusIsTrue(authenticationRequest.getEmailOrPhone(), authenticationRequest.getEmailOrPhone()).orElseThrow(()->
                new BadRequestException("No account found. Please register first."));

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authenticationRequest.getEmailOrPhone(),
                        authenticationRequest.getPassword()
                )
        );
        String userRole = authentication.getAuthorities().iterator().next().getAuthority();
        String name = authentication.getName();

        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        // C. Create HttpOnly Cookies
        // Access Token: Short lived (e.g., 15 mins), available to all paths ("/")
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", jwtToken)
                .httpOnly(true)
                .secure(false) // Set to TRUE in production (HTTPS)
                .path("/")
                .maxAge(15 * 60) // 15 minutes
                .sameSite("Strict")
                .build();

        // Refresh Token: Long lived (e.g., 7 days), specific path only
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false) // Set to TRUE in production
                .path("/api/v1/user/refreshAccessToken") // Only sent to refresh endpoint
                .maxAge(7 * 24 * 60 * 60) // 7 days
                .sameSite("Strict")
                .build();

        // D. Add Cookies to Headers
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        return LoginResponse.builder()
                .name(name)
                .role(userRole)
                .build();
    }

    @Override
    public LoginResponse refreshToken(HttpServletRequest request, HttpServletResponse servletResponse) {

        String refreshToken = null;

        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        // If cookie is missing, throw exception
        if (refreshToken == null) {
            throw new UnauthorizedException("Refresh Token is missing from Cookies");
        }

        // --- 2. EXISTING LOGIC (Validate & Generate) ---
        final String username = jwtService.extractUsername(refreshToken);

        if (username != null) {
            var userDetails = this.userRepository.findByEmailOrPhoneAndStatusIsTrue(username, username)
                    .orElseThrow(() -> new BadRequestException("User not found"));

            if (jwtService.isTokenValid(refreshToken, userDetails)) {
                // Generate NEW Access Token
                var newAccessToken = jwtService.generateToken(userDetails);

                // --- 3. SET NEW ACCESS TOKEN COOKIE ---
                ResponseCookie newAccessCookie = ResponseCookie.from("accessToken", newAccessToken)
                        .httpOnly(true)
                        .secure(false) // Set to true in production
                        .path("/")
                        .maxAge(15 * 60) // 15 minutes
                        .sameSite("Strict")
                        .build();

                servletResponse.addHeader(HttpHeaders.SET_COOKIE, newAccessCookie.toString());

                // Return empty response (Browser has the cookie now)
                return LoginResponse.builder()
                        .name(userDetails.getName())
                        .role(userDetails.getRoles().iterator().next().name())
                        .build();

            } else {
                throw new ForbiddenException("Invalid Refresh Token");
            }
        } else {
            throw new BadRequestException("Invalid Token Claims");
        }
    }

}

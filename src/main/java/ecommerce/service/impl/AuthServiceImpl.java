package ecommerce.service.impl;

import ecommerce.config.JwtService;
import ecommerce.dto.*;
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

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    /*@Override
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
    }*/

    @Override
    public RegistrationResponseDto register(RegistrationRequestDto requestDto) {
        ContactValidator.ContactType type = ContactValidator.identifyContactType(requestDto.getEmailOrPhone());

        // 1. Check if user already exists (Active or Unverified)
        Optional<User> existingUserOpt = userRepository.findByEmailOrPhone(requestDto.getEmailOrPhone(), requestDto.getEmailOrPhone());

        User user;
        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            if (user.getStatus()) {
                // User exists and is already verified
                throw new BadRequestException("This email or phone number already exists.");
            }
            // If user exists but status is false, we just reuse the entity and generate a new OTP
        } else {
            user = new User();
        }

        // 2. Set user details
        user.setName(requestDto.getName());
        user.setPassword(passwordEncoder.encode(requestDto.getPassword()));
        user.getRoles().add(Role.USER);
        user.setStatus(false); // Make sure they are unverified

        switch (type) {
            case EMAIL:
                user.setEmail(requestDto.getEmailOrPhone());
                break;
            case PHONE:
                user.setPhone(requestDto.getEmailOrPhone());
                break;
            case INVALID:
                throw new BadRequestException("Invalid email or phone number format");
        }

        // 3. Generate and set OTP
        String otp = generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5)); // OTP valid for 5 minutes

        userRepository.save(user);

        // TODO: In production, integrate Email Sender or SMS Gateway here.
        // For now, we print it to the console so you can test it!
        System.out.println("=============================================");
        System.out.println("MOCK SENDING OTP: " + otp + " TO: " + requestDto.getEmailOrPhone());
        System.out.println("=============================================");

        return new RegistrationResponseDto("OTP sent to your contact method.");
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

    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // Generates a number between 100000 and 999999
        return String.valueOf(otp);
    }

    public String verifyRegistrationOtp(VerifyOtpRequest request) {
        // 1. Find the user
        User user = userRepository.findByEmailOrPhone(request.getEmailOrPhone(), request.getEmailOrPhone())
                .orElseThrow(() -> new BadRequestException("User not found."));

        // 2. Check if already verified
        if (user.getStatus()) {
            throw new BadRequestException("Account is already verified. Please login.");
        }

        // 3. Check OTP match
        if (user.getOtp() == null || !user.getOtp().equals(request.getOtp())) {
            throw new BadRequestException("Invalid OTP.");
        }

        // 4. Check OTP expiry
        if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }

        // 5. Verification Success: Activate user and clear OTP
        user.setStatus(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        return "Account verified successfully.";
    }

}

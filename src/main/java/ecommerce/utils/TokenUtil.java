package ecommerce.utils;

import ecommerce.config.JwtService;
import ecommerce.entity.User;
import ecommerce.exceptionHandling.BadRequestException;
import ecommerce.exceptionHandling.UnauthorizedException;
import ecommerce.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TokenUtil {
    private final JwtService jwtService;
    private final UserRepository userRepository;

    /**
     * EXTRACT USER INFO
     * Now accepts the Request object so it can check Cookies AND Headers
     */
    public User extractUserInfo(HttpServletRequest request) {
        String jwt = resolveToken(request);

        if (jwt == null) {
            throw new UnauthorizedException("Token not found in Cookie or Header");
        }

        // Now we have the raw JWT (no "Bearer " prefix)
        String username = jwtService.extractUsername(jwt);

        return userRepository.findByEmailOrPhoneAndStatusIsTrue(username, username)
                .orElseThrow(() -> new BadRequestException("User not found"));
    }

    /**
     * HELPER: Finds the raw JWT string
     */
    private String resolveToken(HttpServletRequest request) {
        // 1. Priority: Check for "accessToken" Cookie (Frontend Web)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue(); // Cookies usually contain RAW token
                }
            }
        }

        // 2. Fallback: Check Authorization Header (Mobile / Postman)
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Remove "Bearer " prefix
        }

        return null;
    }
}

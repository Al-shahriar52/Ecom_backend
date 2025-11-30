/*

package ecommerce.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import ecommerce.config.JwtService;
import ecommerce.dto.GenericResponseDto;
import ecommerce.service.impl.TokenBlacklistService; // Corrected import
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        // Step 1: Check for token. If missing, pass to next filter and exit.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final String jti = jwtService.extractJti(jwt);

            // Step 2: Check if token is blacklisted
            if (tokenBlacklistService.isTokenBlacklisted(jti) != null) {
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "This token has been revoked.");
                return;
            }

            // Step 3: Process the token
            final String userEmail = jwtService.extractUsername(jwt);
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            // Step 4: Handle expired token exception
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "JWT Token has expired.");
        } catch (Exception e) {
            // Catch any other potential JWT parsing errors
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid Token.");
        }
    }

    // Helper method to create and send a consistent error response
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        GenericResponseDto<?> errorResponse = GenericResponseDto.error(
                status.getReasonPhrase(), message, status.value()
        );
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        new ObjectMapper().writeValue(response.getOutputStream(), errorResponse);
    }
}*/


package ecommerce.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import ecommerce.dto.GenericResponseDto;
import ecommerce.service.impl.TokenBlacklistService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Step 1: Extract Token from EITHER Cookie OR Header
        String jwt = getTokenFromRequest(request);

        // If no token found in either place, continue without authentication
        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Step 2: Extract JTI and Check Blacklist
            final String jti = jwtService.extractJti(jwt);

            if (tokenBlacklistService.isTokenBlacklisted(jti) != null) {
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "This token has been revoked (Logged out).");
                return;
            }

            // Step 3: Extract User Email
            final String userEmail = jwtService.extractUsername(jwt);

            // Step 4: Validate and Set Security Context
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            // Continue the filter chain
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            // Handle expired token specifically
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "JWT Token has expired.");
        } catch (Exception e) {
            // Handle other parsing errors (malformed token, signature invalid, etc.)
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid Authentication Token.");
        }
    }

    /**
     * HELPER METHOD: Extracts JWT from Cookie first, then falls back to Header.
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        // 1. Try to get from Cookie (Best for Web App security)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // 2. Fallback: Try to get from Authorization Header (Best for Mobile/Postman)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }

    // Helper method to create and send a consistent error response
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        GenericResponseDto<?> errorResponse = GenericResponseDto.error(
                status.getReasonPhrase(), message, status.value()
        );
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        new ObjectMapper().writeValue(response.getOutputStream(), errorResponse);
    }
}
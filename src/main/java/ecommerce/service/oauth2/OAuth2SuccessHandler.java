package ecommerce.service.oauth2;

import ecommerce.config.JwtService;
import ecommerce.entity.User;
import ecommerce.repository.UserRepository;
import ecommerce.service.ActivityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * After a successful Google/Facebook login: issue the SAME HttpOnly cookies the normal
 * email/password login issues (accessToken + refreshToken), then send the browser back to the frontend.
 * No token is ever put in a URL.
 */
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ActivityService activityService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // Set to true in production (HTTPS), same as the "secure" flag in AuthServiceImpl.login
    @Value("${app.oauth2.cookie-secure:false}")
    private boolean cookieSecure;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        Long userId = principal.getAttribute("appUserId");
        User user = userId == null ? null : userRepository.findById(userId).orElse(null);

        if (user == null) {
            getRedirectStrategy().sendRedirect(request, response,
                    UriComponentsBuilder.fromUriString(frontendUrl + "/login")
                            .queryParam("error", "Social login failed. Please try again.")
                            .build().encode().toUriString());
            return;
        }

        // Same cookie settings as AuthServiceImpl.login
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", jwtService.generateToken(user))
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(60 * 60)
                .sameSite("Strict")
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", jwtService.generateRefreshToken(user))
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/v1/user/refreshAccessToken")
                .maxAge(28 * 24 * 60 * 60)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        activityService.logActivity(user.getId(), "User logged in via social login");

        // The app is stateless: drop the temporary session that was only needed for the OAuth2 handshake.
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/oauth2/callback");
    }
}

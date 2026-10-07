package ecommerce.service.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Set;

/**
 * On any social-login failure, send the browser to the frontend login page with a readable message.
 */
@Slf4j
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    // Messages we wrote ourselves and are safe to show to the user.
    private static final Set<String> SAFE_CODES = Set.of(
            "email_not_found", "email_exists", "account_suspended", "invalid_profile");

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        log.warn("Social login failed: {}", exception.getMessage());

        String message = "Social login failed. Please try again.";
        if (exception instanceof OAuth2AuthenticationException) {
            String code = ((OAuth2AuthenticationException) exception).getError().getErrorCode();
            if ("access_denied".equals(code)) {
                message = "Social login was cancelled.";
            } else if (SAFE_CODES.contains(code)) {
                message = exception.getMessage();
            }
        }

        String target = UriComponentsBuilder.fromUriString(frontendUrl + "/login")
                .queryParam("error", message)
                .build().encode().toUriString();

        getRedirectStrategy().sendRedirect(request, response, target);
    }
}

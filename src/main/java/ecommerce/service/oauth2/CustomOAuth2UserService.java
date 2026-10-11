package ecommerce.service.oauth2;

import ecommerce.entity.User;
import ecommerce.enums.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Called by Spring Security after Google/Facebook return the user's profile.
 * Maps the profile to an application User and exposes its id as "appUserId".
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final SocialAuthService socialAuthService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(request);

        SocialProvider provider = SocialProvider.valueOf(
                request.getClientRegistration().getRegistrationId().toUpperCase());
        Map<String, Object> attrs = oAuth2User.getAttributes();

        // Google identifies the user by "sub", Facebook by "id".
        String idKey = provider == SocialProvider.GOOGLE ? "sub" : "id";
        Object rawId = attrs.get(idKey);
        if (rawId == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_profile"),
                    "Could not read your profile from the login provider.");
        }

        String email = attrs.get("email") == null ? null : String.valueOf(attrs.get("email"));
        String name = attrs.get("name") == null ? null : String.valueOf(attrs.get("name"));
        // Only Google tells us whether the email is verified. Facebook is treated as unverified.
        boolean emailVerified = provider == SocialProvider.GOOGLE
                && "true".equalsIgnoreCase(String.valueOf(attrs.get("email_verified")));

        User user = socialAuthService.findOrCreateUser(
                provider, String.valueOf(rawId), email, emailVerified, name);

        Map<String, Object> enriched = new HashMap<String, Object>(attrs);
        enriched.put("appUserId", user.getId());

        return new DefaultOAuth2User(user.getAuthorities(), enriched, idKey);
    }
}

package ecommerce.service.oauth2;

import ecommerce.entity.AccountState;
import ecommerce.entity.Role;
import ecommerce.entity.SocialAccount;
import ecommerce.entity.User;
import ecommerce.enums.SocialProvider;
import ecommerce.repository.SocialAccountRepository;
import ecommerce.repository.UserRepository;
import ecommerce.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Finds or creates the application User for a Google/Facebook login.
 * Only active when the "oauth2" Spring profile is on.
 */
@Service
@RequiredArgsConstructor
public class SocialAuthService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final ActivityService activityService;

    @Transactional
    public User findOrCreateUser(SocialProvider provider,
                                 String providerId,
                                 String email,
                                 boolean emailVerified,
                                 String name) {

        // 1) Returning social user: matched by the provider's own stable id.
        Optional<SocialAccount> linked = socialAccountRepository.findByProviderAndProviderId(provider, providerId);
        if (linked.isPresent()) {
            User user = linked.get().getUser();
            assertNotSuspended(user);
            return user;
        }

        // 2) First login with this provider: we need an email address.
        if (email == null || email.isBlank()) {
            throw error("email_not_found",
                    "We could not get an email address from " + label(provider)
                            + ". Please use another login method.");
        }
        String cleanEmail = email.trim();

        Optional<User> existing = userRepository.findByEmail(cleanEmail);
        User user;

        if (existing.isPresent()) {
            user = existing.get();
            assertNotSuspended(user);

            // Only link to an existing account when the provider guarantees the email is verified
            // (Google does; for Facebook we do not trust it). Otherwise anyone could take over an
            // existing account by creating a social account with someone else's email.
            if (!emailVerified) {
                throw error("email_exists",
                        "An account with this email already exists. Please log in with your existing method.");
            }

            if (!Boolean.TRUE.equals(user.getStatus())) {
                // Someone started registering with this email but never finished the OTP step.
                // The provider has now verified the email, so activate the account and drop the
                // unverified password so whoever pre-registered it cannot log in with it.
                user.setStatus(true);
                user.setAccountState(AccountState.ACTIVE);
                user.setPassword(null);
                user.setOtp(null);
                user.setOtpExpiry(null);
                userRepository.save(user);
            }
        } else {
            user = new User();
            user.setName(displayName(name, cleanEmail));
            user.setEmail(cleanEmail);
            user.setStatus(true);
            user.setAccountState(AccountState.ACTIVE);
            user.getRoles().add(Role.USER);
            user = userRepository.save(user);
            activityService.logActivity(user.getId(), "User registered via " + label(provider));
        }

        SocialAccount link = new SocialAccount();
        link.setUser(user);
        link.setProvider(provider);
        link.setProviderId(providerId);
        socialAccountRepository.save(link);

        return user;
    }

    private void assertNotSuspended(User user) {
        if (user.getAccountState() == AccountState.SUSPENDED) {
            throw error("account_suspended", "This account has been suspended. Please contact support.");
        }
    }

    private static String displayName(String name, String email) {
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : "User";
    }

    private static String label(SocialProvider provider) {
        return provider == SocialProvider.GOOGLE ? "Google" : "Facebook";
    }

    private static OAuth2AuthenticationException error(String code, String message) {
        return new OAuth2AuthenticationException(new OAuth2Error(code), message);
    }
}

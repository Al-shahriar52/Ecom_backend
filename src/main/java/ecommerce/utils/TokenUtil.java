package ecommerce.utils;

import ecommerce.config.JwtService;
import ecommerce.entity.User;
import ecommerce.exceptionHandling.BadRequestException;
import ecommerce.exceptionHandling.UnauthorizedException;
import ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TokenUtil {
    private final JwtService jwtService;
    private final UserRepository userRepository;
    public User extractUserInfo(String authToken) {
        if (authToken != null && authToken.startsWith("Bearer ")) {
            String jwt = authToken.substring(7);
            String username = jwtService.extractUsername(jwt);

            Optional<User> user = userRepository.findByEmailOrPhoneAndStatusIsTrue(username, username);

            if (user.isPresent()) {
                return user.get();
            }else {
                throw new BadRequestException("User not found");
            }

        }else {
            throw new UnauthorizedException("Invalid token");
        }
    }
}

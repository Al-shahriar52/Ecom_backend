package ecommerce.service.impl;

import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {

    // A simple placeholder value. The presence of the key in the cache is what matters.
    private static final String BLACKLISTED_VALUE = "blacklisted";

    /**
     * Blacklists a token. The @CachePut annotation ensures this method is always
     * executed and its result is placed into the 'blacklistedTokens' cache.
     * The key for the cache is the token's JTI.
     */
    @CachePut(value = "blacklistedTokens", key = "#jti")
    public String blacklistToken(String jti) {
        return BLACKLISTED_VALUE;
    }

    /**
     * Checks if a token is blacklisted. The @Cacheable annotation means the result
     * of this method will be cached. If the JTI is already in the cache, the cached
     * value is returned without executing the method.
     *
     * @return The string "blacklisted" if the token is in the cache, otherwise null.
     */
    @Cacheable(value = "blacklistedTokens", key = "#jti")
    public String isTokenBlacklisted(String jti) {
        // If the token is not in the cache, this method will be executed and return null.
        // The null response will not be cached.
        return null;
    }
}
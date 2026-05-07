package com.corebank.auth.application.service;

import com.corebank.auth.application.port.input.AuthenticateUseCase;
import com.corebank.auth.application.port.output.TokenCachePort;
import com.corebank.auth.domain.model.Credentials;
import com.corebank.commons.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Application service implementing the authentication use case.
 * Orchestrates: credential validation → JWT generation → Redis caching.
 */
@Service
public class AuthApplicationService implements AuthenticateUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuthApplicationService.class);

    private final JwtUtil jwtUtil;
    private final TokenCachePort tokenCachePort;

    public AuthApplicationService(JwtUtil jwtUtil, TokenCachePort tokenCachePort) {
        this.jwtUtil = jwtUtil;
        this.tokenCachePort = tokenCachePort;
    }

    @Override
    public String authenticate(String username, String password,
                               String custIdentNum, String custIdentType) {

        Credentials credentials = new Credentials(username, password);

        if (!credentials.isValid()) {
            log.warn("Authentication failed for user: {}", username);
            throw new SecurityException("Invalid credentials");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("custIdentNum", custIdentNum != null ? custIdentNum : "123456789");
        claims.put("custIdentType", custIdentType != null ? custIdentType : "CC");
        claims.put("X-SesID", "session-" + System.currentTimeMillis());

        String token = jwtUtil.generateToken(credentials.getUsername(), claims);

        // Cache the token in Redis (1 hour TTL)
        String cacheKey = "auth:token:" + credentials.getUsername();
        tokenCachePort.cacheToken(cacheKey, token, 3600);

        log.info("Authentication successful for user: {}, custIdentNum: {}", username, custIdentNum);
        return token;
    }
}

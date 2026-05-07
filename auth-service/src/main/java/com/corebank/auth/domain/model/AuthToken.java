package com.corebank.auth.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * Domain value object representing an authentication token.
 */
@Getter
@Builder
@AllArgsConstructor
public class AuthToken {
    private final String token;
    private final String username;
    private final Instant expiresAt;
    private final Map<String, Object> claims;
}

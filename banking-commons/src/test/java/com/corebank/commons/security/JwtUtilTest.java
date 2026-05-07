package com.corebank.commons.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "super-secret-for-demo-only-change-in-prod");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L);
    }

    @Test
    @DisplayName("generateToken should return a non-null JWT string")
    void generateTokenShouldReturnNonNullString() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("custIdentNum", "123456789");

        String token = jwtUtil.generateToken("testuser", claims);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts
    }

    @Test
    @DisplayName("validateToken should return true for a valid token")
    void validateTokenShouldReturnTrueForValidToken() {
        String token = jwtUtil.generateToken("testuser", Map.of());

        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("validateToken should return false for an invalid token")
    void validateTokenShouldReturnFalseForInvalidToken() {
        assertFalse(jwtUtil.validateToken("invalid.token.here"));
    }

    @Test
    @DisplayName("validateToken should return false for null token")
    void validateTokenShouldReturnFalseForNull() {
        assertFalse(jwtUtil.validateToken(null));
    }

    @Test
    @DisplayName("extractUsername should return the subject from token")
    void extractUsernameShouldReturnSubject() {
        String token = jwtUtil.generateToken("testuser", Map.of());

        assertEquals("testuser", jwtUtil.extractUsername(token));
    }

    @Test
    @DisplayName("extractAllClaims should return custom claims")
    void extractAllClaimsShouldReturnCustomClaims() {
        Map<String, Object> customClaims = new HashMap<>();
        customClaims.put("custIdentNum", "123456789");
        customClaims.put("custIdentType", "CC");

        String token = jwtUtil.generateToken("testuser", customClaims);
        Claims claims = jwtUtil.extractAllClaims(token);

        assertEquals("testuser", claims.getSubject());
        assertEquals("123456789", claims.get("custIdentNum"));
        assertEquals("CC", claims.get("custIdentType"));
    }

    @Test
    @DisplayName("Expired token should fail validation")
    void expiredTokenShouldFailValidation() {
        JwtUtil shortLivedUtil = new JwtUtil();
        ReflectionTestUtils.setField(shortLivedUtil, "secret", "super-secret-for-demo-only-change-in-prod");
        ReflectionTestUtils.setField(shortLivedUtil, "expiration", -1000L); // Already expired

        String token = shortLivedUtil.generateToken("testuser", Map.of());

        assertFalse(jwtUtil.validateToken(token));
    }
}

package com.corebank.auth.application.service;

import com.corebank.auth.application.port.output.TokenCachePort;
import com.corebank.commons.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthApplicationServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenCachePort tokenCachePort;

    @InjectMocks
    private AuthApplicationService authService;

    @Test
    @DisplayName("authenticate should return token for valid credentials")
    void authenticateShouldReturnTokenForValidCredentials() {
        when(jwtUtil.generateToken(eq("user"), anyMap())).thenReturn("mock-jwt-token");

        String token = authService.authenticate("user", "password", "123456789", "CC");

        assertNotNull(token);
        assertEquals("mock-jwt-token", token);
        verify(jwtUtil).generateToken(eq("user"), anyMap());
        verify(tokenCachePort).cacheToken(eq("auth:token:user"), eq("mock-jwt-token"), eq(3600L));
    }

    @Test
    @DisplayName("authenticate should throw SecurityException for invalid credentials")
    void authenticateShouldThrowForInvalidCredentials() {
        assertThrows(SecurityException.class,
                () -> authService.authenticate("wrong", "wrong", "123456789", "CC"));

        verify(jwtUtil, never()).generateToken(anyString(), anyMap());
        verify(tokenCachePort, never()).cacheToken(anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("authenticate should use default custIdentNum when null")
    void authenticateShouldUseDefaultCustIdentNumWhenNull() {
        when(jwtUtil.generateToken(eq("user"), anyMap())).thenReturn("mock-jwt-token");

        String token = authService.authenticate("user", "password", null, null);

        assertNotNull(token);
        verify(jwtUtil).generateToken(eq("user"), argThat(claims ->
                "123456789".equals(claims.get("custIdentNum")) &&
                        "CC".equals(claims.get("custIdentType"))
        ));
    }

    @Test
    @DisplayName("authenticate should cache token with correct key")
    void authenticateShouldCacheTokenWithCorrectKey() {
        when(jwtUtil.generateToken(eq("user"), anyMap())).thenReturn("cached-token");

        authService.authenticate("user", "password", "123456789", "CC");

        verify(tokenCachePort).cacheToken("auth:token:user", "cached-token", 3600L);
    }

    @Test
    @DisplayName("authenticate should include session ID in claims")
    void authenticateShouldIncludeSessionIdInClaims() {
        when(jwtUtil.generateToken(eq("user"), anyMap())).thenReturn("token");

        authService.authenticate("user", "password", "123456789", "CC");

        verify(jwtUtil).generateToken(eq("user"), argThat(claims ->
                claims.containsKey("X-SesID") &&
                        claims.get("X-SesID").toString().startsWith("session-")
        ));
    }
}

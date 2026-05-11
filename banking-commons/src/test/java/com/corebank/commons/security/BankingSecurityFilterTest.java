package com.corebank.commons.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankingSecurityFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private BankingSecurityFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    @Test
    void testShouldNotFilter_LoginPath() throws ServletException, IOException {
        request.setRequestURI("/api/auth/login");

        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testShouldNotFilter_ActuatorPath() throws ServletException, IOException {
        request.setRequestURI("/actuator/health");

        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_MissingAuthHeader() throws ServletException, IOException {
        request.setRequestURI("/api/home/balance");

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_InvalidToken() throws ServletException, IOException {
        request.setRequestURI("/api/home/balance");
        request.addHeader(HeaderConstants.AUTHORIZATION, HeaderConstants.BEARER_PREFIX + "invalid.token");

        when(jwtUtil.validateToken("invalid.token")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_ValidToken() throws ServletException, IOException {
        request.setRequestURI("/api/home/balance");
        request.addHeader(HeaderConstants.AUTHORIZATION, HeaderConstants.BEARER_PREFIX + "valid.token");

        when(jwtUtil.validateToken("valid.token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid.token")).thenReturn("testuser");

        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("testuser", SecurityContextHolder.getContext().getAuthentication().getName());
        assertNotNull(request.getAttribute(HeaderConstants.X_RQ_UID));
    }
}

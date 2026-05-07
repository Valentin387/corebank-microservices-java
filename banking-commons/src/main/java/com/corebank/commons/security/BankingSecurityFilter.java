package com.corebank.commons.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

/**
 * Servlet-based (MVC) security filter for JWT validation and banking header enrichment.
 * Used by auth-service and any MVC-based microservice.
 *
 * Extracted and improved from the monolith's JwtFilter.
 */
public class BankingSecurityFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public BankingSecurityFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/api/auth/login") || path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(HeaderConstants.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith(HeaderConstants.BEARER_PREFIX)) {
            String token = authHeader.substring(HeaderConstants.BEARER_PREFIX.length());

            if (jwtUtil.validateToken(token)) {
                String username = jwtUtil.extractUsername(token);
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                // Enrich X-RqUid if not present (traceability)
                if (request.getHeader(HeaderConstants.X_RQ_UID) == null) {
                    request.setAttribute(HeaderConstants.X_RQ_UID, UUID.randomUUID().toString());
                }

                filterChain.doFilter(request, response);
                return;
            }
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}

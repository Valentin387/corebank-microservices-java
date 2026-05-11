package com.corebank.commons.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactiveJwtFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private WebFilterChain filterChain;

    @InjectMocks
    private ReactiveJwtFilter filter;

    @Test
    void testFilter_LoginPath() {
        when(exchange.getRequest()).thenReturn(request);
        when(request.getURI()).thenReturn(URI.create("/api/auth/login"));
        when(filterChain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, filterChain).block();

        verify(filterChain).filter(exchange);
    }

    @Test
    void testFilter_ActuatorPath() {
        when(exchange.getRequest()).thenReturn(request);
        when(request.getURI()).thenReturn(URI.create("/actuator/health"));
        when(filterChain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, filterChain).block();

        verify(filterChain).filter(exchange);
    }

    @Test
    void testFilter_MissingAuthHeader() {
        when(exchange.getRequest()).thenReturn(request);
        when(request.getURI()).thenReturn(URI.create("/api/home/balance"));

        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        when(exchange.getResponse()).thenReturn(response);
        when(response.setComplete()).thenReturn(Mono.empty());

        filter.filter(exchange, filterChain).block();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(filterChain, never()).filter(any());
    }

    @Test
    void testFilter_InvalidToken() {
        when(exchange.getRequest()).thenReturn(request);
        when(request.getURI()).thenReturn(URI.create("/api/home/balance"));

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, HeaderConstants.BEARER_PREFIX + "invalid.token");
        when(request.getHeaders()).thenReturn(headers);
        when(exchange.getResponse()).thenReturn(response);
        when(response.setComplete()).thenReturn(Mono.empty());
        when(jwtUtil.validateToken("invalid.token")).thenReturn(false);

        filter.filter(exchange, filterChain).block();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(filterChain, never()).filter(any());
    }

    @Test
    void testFilter_ValidToken() {
        when(exchange.getRequest()).thenReturn(request);
        when(request.getURI()).thenReturn(URI.create("/api/home/balance"));

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, HeaderConstants.BEARER_PREFIX + "valid.token");
        when(request.getHeaders()).thenReturn(headers);
        when(jwtUtil.validateToken("valid.token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid.token")).thenReturn("testuser");

        when(filterChain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, filterChain).block();

        verify(filterChain).filter(exchange);
    }
}

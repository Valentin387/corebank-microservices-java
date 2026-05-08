package com.corebank.auth.infrastructure.adapter.input.web;

import com.corebank.auth.application.port.input.AuthenticateUseCase;
import com.corebank.commons.exception.GlobalExceptionHandler;
import com.corebank.commons.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({AuthController.class, AuthControllerTest.TestSecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticateUseCase authenticateUseCase;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Configuration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Test
    @DisplayName("POST /api/auth/login should return 200 with JWT token")
    void loginShouldReturn200WithToken() throws Exception {
        when(authenticateUseCase.authenticate(eq("user"), eq("password"), eq("123456789"), eq("CC")))
                .thenReturn("mock-jwt-token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-CustIdentNum", "123456789")
                        .header("X-CustIdentType", "CC")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "user",
                                "password", "password"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.body").value("mock-jwt-token"));
    }

    @Test
    @DisplayName("POST /api/auth/login without headers should still work")
    void loginWithoutHeadersShouldStillWork() throws Exception {
        when(authenticateUseCase.authenticate(eq("user"), eq("password"), isNull(), isNull()))
                .thenReturn("mock-jwt-token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "user",
                                "password", "password"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.body").value("mock-jwt-token"));
    }

    @Test
    @DisplayName("POST /api/auth/login with invalid credentials should return error")
    void loginWithInvalidCredentialsShouldReturnError() throws Exception {
        when(authenticateUseCase.authenticate(anyString(), anyString(), any(), any()))
                .thenThrow(new SecurityException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "wrong",
                                "password", "wrong"
                        ))))
                .andExpect(status().isUnauthorized());
    }
}

package com.corebank.auth.infrastructure.config.security;

import com.corebank.auth.infrastructure.config.SecurityConfig;
import com.corebank.commons.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SecurityConfigTest.TestController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @SpringBootApplication
    @RestController
    static class TestController {
        @GetMapping("/api/auth/test")
        public String authTest() {
            return "permitted";
        }

        @GetMapping("/actuator/health")
        public String actuatorTest() {
            return "permitted";
        }

        @GetMapping("/api/secured/test")
        public String securedTest() {
            return "secured";
        }
    }

    @Test
    @DisplayName("Access to /api/auth/** should be permitted without authentication")
    void whenAccessingAuthEndpoint_thenPermitAll() throws Exception {
        mockMvc.perform(get("/api/auth/test"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Access to /actuator/** should be permitted without authentication")
    void whenAccessingActuatorEndpoint_thenPermitAll() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Access to any other endpoint without a token should return 401 Unauthorized")
    void whenAccessingSecuredEndpointWithoutToken_thenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/secured/test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Access to secured endpoint with a valid JWT token should return 200 OK")
    void whenAccessingSecuredEndpointWithValidToken_thenOk() throws Exception {
        String token = "valid-token";
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.extractUsername(token)).thenReturn("user");

        mockMvc.perform(get("/api/secured/test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Access to secured endpoint with an invalid JWT token should return 401 Unauthorized")
    void whenAccessingSecuredEndpointWithInvalidToken_thenUnauthorized() throws Exception {
        String token = "invalid-token";
        when(jwtUtil.validateToken(token)).thenReturn(false);

        mockMvc.perform(get("/api/secured/test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}

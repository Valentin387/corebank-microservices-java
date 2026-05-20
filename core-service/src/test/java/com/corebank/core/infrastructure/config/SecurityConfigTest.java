package com.corebank.core.infrastructure.config;

import com.corebank.commons.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration",
                "spring.sql.init.mode=never"
        }
)
@AutoConfigureWebTestClient
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private JwtUtil jwtUtil;

    @RestController
    static class TestController {

        @GetMapping("/actuator/health")
        public String actuatorTest() {
            return "permitted";
        }

        @GetMapping("/api/core/test")
        public String securedTest() {
            return "secured";
        }
    }

    @Test
    @DisplayName("Access to secured endpoint with a valid JWT token should return 200 OK")
    void whenAccessingSecuredEndpointWithValidToken_thenOk() {
        String token = "valid-token";
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.extractUsername(token)).thenReturn("user");

        webTestClient.get().uri("/api/core/test")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("secured");
    }
}
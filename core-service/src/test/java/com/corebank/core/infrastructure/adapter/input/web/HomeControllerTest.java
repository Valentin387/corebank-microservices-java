package com.corebank.core.infrastructure.adapter.input.web;

import com.corebank.commons.security.JwtUtil;
import com.corebank.core.application.port.input.GetHomeBalanceUseCase;
import com.corebank.core.domain.model.Account;
import com.corebank.core.domain.model.Balance;
import com.corebank.core.domain.model.Card;
import com.corebank.core.domain.model.HomeAggregate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import com.corebank.commons.exception.GlobalExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(HomeController.class)
@Import({HomeController.class, HomeControllerTest.TestSecurityConfig.class, GlobalExceptionHandler.class})
class HomeControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private GetHomeBalanceUseCase getHomeBalanceUseCase;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Configuration
    static class TestSecurityConfig {
        @Bean
        public SecurityWebFilterChain testFilterChain(ServerHttpSecurity http) {
            return http
                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                    .build();
        }
    }

    @Test
    @DisplayName("GET /api/home/balance should return aggregated data")
    void getBalanceShouldReturnAggregatedData() {
        HomeAggregate aggregate = HomeAggregate.builder()
                .accounts(List.of(
                        Account.builder().id(1L).accountNumber("ACC-001")
                                .accountType("SAVINGS").balance(new BigDecimal("1000.00"))
                                .customerId("123456789").build()
                ))
                .cards(List.of(
                        Card.builder().id(1L).cardNumber("CARD-001")
                                .cardType("CREDIT").creditLimit(new BigDecimal("5000.00"))
                                .availableBalance(new BigDecimal("3200.00"))
                                .customerId("123456789").build()
                ))
                .balance(Balance.builder()
                        .customerId("123456789")
                        .totalBalance(new BigDecimal("3500.50"))
                        .availableBalance(new BigDecimal("3200.00")).build())
                .build();

        when(getHomeBalanceUseCase.getAggregatedBalance(anyString())).thenReturn(Mono.just(aggregate));

        webTestClient.get()
                .uri("/api/home/balance")
                .header("X-CustIdentNum", "123456789")
                .header("X-CustIdentType", "CC")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.statusCode").isEqualTo(200)
                .jsonPath("$.body.accounts").isArray()
                .jsonPath("$.body.accounts[0].accountNumber").isEqualTo("ACC-001")
                .jsonPath("$.body.cards").isArray()
                .jsonPath("$.body.cards[0].cardNumber").isEqualTo("CARD-001")
                .jsonPath("$.body.balance.customerId").isEqualTo("123456789");
    }

    @Test
    @DisplayName("GET /api/home/balance should handle empty data")
    void getBalanceShouldHandleEmptyData() {
        HomeAggregate emptyAggregate = HomeAggregate.builder()
                .accounts(List.of())
                .cards(List.of())
                .balance(null)
                .build();

        when(getHomeBalanceUseCase.getAggregatedBalance(anyString())).thenReturn(Mono.just(emptyAggregate));

        webTestClient.get()
                .uri("/api/home/balance")
                .header("X-CustIdentNum", "999999999")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.statusCode").isEqualTo(200)
                .jsonPath("$.body.accounts").isArray()
                .jsonPath("$.body.accounts").isEmpty();
    }
}

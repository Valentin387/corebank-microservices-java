package com.corebank.core.infrastructure.adapter.output.persistence;

import com.corebank.core.domain.model.Balance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceRepositoryAdapterTest {

    @Mock
    private BalanceJpaRepository jpaRepository;

    @InjectMocks
    private BalanceRepositoryAdapter adapter;

    @Test
    @DisplayName("findByCustomerId should return balance reactively")
    void findByCustomerIdShouldReturnBalanceReactively() {
        Balance balance = Balance.builder()
                .id(1L).customerId("123456789")
                .totalBalance(new BigDecimal("3500.50"))
                .availableBalance(new BigDecimal("3200.00"))
                .build();

        when(jpaRepository.findByCustomerId("123456789")).thenReturn(Optional.of(balance));

        StepVerifier.create(adapter.findByCustomerId("123456789"))
                .expectNextMatches(b -> b.getCustomerId().equals("123456789"))
                .verifyComplete();
    }

    @Test
    @DisplayName("findByCustomerId should emit empty when not found")
    void findByCustomerIdShouldEmitEmptyWhenNotFound() {
        when(jpaRepository.findByCustomerId("999")).thenReturn(Optional.empty());

        StepVerifier.create(adapter.findByCustomerId("999"))
                .expectNextCount(0) // Mono.just(null) emits null then completes
                .verifyComplete();
    }
}

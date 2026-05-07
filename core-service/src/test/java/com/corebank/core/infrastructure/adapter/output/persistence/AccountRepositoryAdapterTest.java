package com.corebank.core.infrastructure.adapter.output.persistence;

import com.corebank.core.domain.model.Account;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountRepositoryAdapterTest {

    @Mock
    private AccountJpaRepository jpaRepository;

    @InjectMocks
    private AccountRepositoryAdapter adapter;

    @Test
    @DisplayName("findByCustomerId should return accounts reactively")
    void findByCustomerIdShouldReturnAccountsReactively() {
        List<Account> accounts = List.of(
                Account.builder().id(1L).accountNumber("ACC-001").accountType("SAVINGS")
                        .balance(new BigDecimal("1000.00")).customerId("123456789").build()
        );

        when(jpaRepository.findByCustomerId("123456789")).thenReturn(accounts);

        StepVerifier.create(adapter.findByCustomerId("123456789"))
                .expectNextMatches(result -> result.size() == 1 && result.get(0).getAccountNumber().equals("ACC-001"))
                .verifyComplete();
    }

    @Test
    @DisplayName("findByCustomerId should return empty list when no accounts found")
    void findByCustomerIdShouldReturnEmptyList() {
        when(jpaRepository.findByCustomerId("999")).thenReturn(List.of());

        StepVerifier.create(adapter.findByCustomerId("999"))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
    }
}

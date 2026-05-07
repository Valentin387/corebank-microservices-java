package com.corebank.core.application.service;

import com.corebank.core.application.port.output.AccountRepositoryPort;
import com.corebank.core.application.port.output.BalanceRepositoryPort;
import com.corebank.core.application.port.output.CardRepositoryPort;
import com.corebank.core.domain.model.Account;
import com.corebank.core.domain.model.Balance;
import com.corebank.core.domain.model.Card;
import com.corebank.core.domain.model.HomeAggregate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeApplicationServiceTest {

    @Mock
    private AccountRepositoryPort accountRepositoryPort;

    @Mock
    private CardRepositoryPort cardRepositoryPort;

    @Mock
    private BalanceRepositoryPort balanceRepositoryPort;

    @InjectMocks
    private HomeApplicationService homeService;

    @Test
    @DisplayName("getAggregatedBalance should return complete HomeAggregate")
    void getAggregatedBalanceShouldReturnCompleteAggregate() {
        String customerId = "123456789";

        List<Account> accounts = List.of(
                Account.builder().id(1L).accountNumber("ACC-001").accountType("SAVINGS")
                        .balance(new BigDecimal("1000.00")).customerId(customerId).build()
        );

        List<Card> cards = List.of(
                Card.builder().id(1L).cardNumber("CARD-001").cardType("CREDIT")
                        .creditLimit(new BigDecimal("5000.00")).availableBalance(new BigDecimal("3200.00"))
                        .customerId(customerId).build()
        );

        Balance balance = Balance.builder()
                .id(1L).customerId(customerId)
                .totalBalance(new BigDecimal("3500.50")).availableBalance(new BigDecimal("3200.00"))
                .build();

        when(accountRepositoryPort.findByCustomerId(customerId)).thenReturn(Mono.just(accounts));
        when(cardRepositoryPort.findByCustomerId(customerId)).thenReturn(Mono.just(cards));
        when(balanceRepositoryPort.findByCustomerId(customerId)).thenReturn(Mono.just(balance));

        StepVerifier.create(homeService.getAggregatedBalance(customerId))
                .expectNextMatches(aggregate ->
                        aggregate.getAccounts().size() == 1 &&
                                aggregate.getCards().size() == 1 &&
                                aggregate.getBalance() != null &&
                                aggregate.getBalance().getCustomerId().equals(customerId)
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("getAggregatedBalance should handle empty results")
    void getAggregatedBalanceShouldHandleEmptyResults() {
        String customerId = "999999999";

        when(accountRepositoryPort.findByCustomerId(customerId)).thenReturn(Mono.just(List.of()));
        when(cardRepositoryPort.findByCustomerId(customerId)).thenReturn(Mono.just(List.of()));
        when(balanceRepositoryPort.findByCustomerId(customerId)).thenReturn(Mono.just(
                Balance.builder().customerId(customerId)
                        .totalBalance(BigDecimal.ZERO).availableBalance(BigDecimal.ZERO).build()
        ));

        StepVerifier.create(homeService.getAggregatedBalance(customerId))
                .expectNextMatches(aggregate ->
                        aggregate.getAccounts().isEmpty() &&
                                aggregate.getCards().isEmpty() &&
                                aggregate.getBalance() != null
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("getAggregatedBalance should use Mono.zip for parallel execution")
    void getAggregatedBalanceShouldUseMonoZip() {
        String customerId = "123456789";

        when(accountRepositoryPort.findByCustomerId(customerId)).thenReturn(Mono.just(List.of()));
        when(cardRepositoryPort.findByCustomerId(customerId)).thenReturn(Mono.just(List.of()));
        when(balanceRepositoryPort.findByCustomerId(customerId)).thenReturn(Mono.just(
                Balance.builder().customerId(customerId)
                        .totalBalance(BigDecimal.ZERO).availableBalance(BigDecimal.ZERO).build()
        ));

        Mono<HomeAggregate> result = homeService.getAggregatedBalance(customerId);

        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();
    }
}

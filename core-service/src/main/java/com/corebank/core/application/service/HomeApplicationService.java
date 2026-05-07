package com.corebank.core.application.service;

import com.corebank.core.application.port.input.GetHomeBalanceUseCase;
import com.corebank.core.application.port.output.AccountRepositoryPort;
import com.corebank.core.application.port.output.BalanceRepositoryPort;
import com.corebank.core.application.port.output.CardRepositoryPort;
import com.corebank.core.domain.model.HomeAggregate;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Application service implementing the home balance aggregation use case.
 * Uses Mono.zip() for reactive parallel orchestration of data sources.
 * Protected by Resilience4j Circuit Breaker + Retry.
 */
@Service
public class HomeApplicationService implements GetHomeBalanceUseCase {

    private static final Logger log = LoggerFactory.getLogger(HomeApplicationService.class);

    private final AccountRepositoryPort accountRepositoryPort;
    private final CardRepositoryPort cardRepositoryPort;
    private final BalanceRepositoryPort balanceRepositoryPort;

    public HomeApplicationService(AccountRepositoryPort accountRepositoryPort,
                                  CardRepositoryPort cardRepositoryPort,
                                  BalanceRepositoryPort balanceRepositoryPort) {
        this.accountRepositoryPort = accountRepositoryPort;
        this.cardRepositoryPort = cardRepositoryPort;
        this.balanceRepositoryPort = balanceRepositoryPort;
    }

    @Override
    @CircuitBreaker(name = "homeService", fallbackMethod = "getAggregatedBalanceFallback")
    @Retry(name = "homeService")
    public Mono<HomeAggregate> getAggregatedBalance(String customerId) {
        log.info("Fetching aggregated balance for customer: {}", customerId);

        return Mono.zip(
                accountRepositoryPort.findByCustomerId(customerId),
                cardRepositoryPort.findByCustomerId(customerId),
                balanceRepositoryPort.findByCustomerId(customerId)
        ).map(tuple -> HomeAggregate.builder()
                .accounts(tuple.getT1())
                .cards(tuple.getT2())
                .balance(tuple.getT3())
                .build()
        );
    }

    /**
     * Fallback method when circuit breaker is open.
     */
    @SuppressWarnings("unused")
    private Mono<HomeAggregate> getAggregatedBalanceFallback(String customerId, Throwable t) {
        log.warn("Circuit breaker fallback triggered for customer: {}, reason: {}", customerId, t.getMessage());
        return Mono.just(HomeAggregate.builder()
                .accounts(java.util.Collections.emptyList())
                .cards(java.util.Collections.emptyList())
                .balance(null)
                .build());
    }
}

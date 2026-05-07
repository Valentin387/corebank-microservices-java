package com.corebank.core.infrastructure.adapter.output.persistence;

import com.corebank.core.application.port.output.BalanceRepositoryPort;
import com.corebank.core.domain.model.Balance;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Adapter bridging the reactive BalanceRepositoryPort to the blocking JPA repository.
 * Uses Schedulers.boundedElastic() to prevent blocking the event loop.
 */
@Component
public class BalanceRepositoryAdapter implements BalanceRepositoryPort {

    private final BalanceJpaRepository jpaRepository;

    public BalanceRepositoryAdapter(BalanceJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Mono<Balance> findByCustomerId(String customerId) {
        return Mono.fromCallable(() -> jpaRepository.findByCustomerId(customerId).orElse(null))
                .subscribeOn(Schedulers.boundedElastic());
    }
}

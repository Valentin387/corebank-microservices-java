package com.corebank.core.infrastructure.adapter.output.persistence;

import com.corebank.core.application.port.output.AccountRepositoryPort;
import com.corebank.core.domain.model.Account;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * Adapter bridging the reactive AccountRepositoryPort to the blocking JPA repository.
 * Uses Schedulers.boundedElastic() to prevent blocking the event loop.
 */
@Component
public class AccountRepositoryAdapter implements AccountRepositoryPort {

    private final AccountJpaRepository jpaRepository;

    public AccountRepositoryAdapter(AccountJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Mono<List<Account>> findByCustomerId(String customerId) {
        return Mono.fromCallable(() -> jpaRepository.findByCustomerId(customerId))
                .subscribeOn(Schedulers.boundedElastic());
    }
}

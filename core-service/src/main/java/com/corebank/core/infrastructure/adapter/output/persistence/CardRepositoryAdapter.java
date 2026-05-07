package com.corebank.core.infrastructure.adapter.output.persistence;

import com.corebank.core.application.port.output.CardRepositoryPort;
import com.corebank.core.domain.model.Card;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * Adapter bridging the reactive CardRepositoryPort to the blocking JPA repository.
 * Uses Schedulers.boundedElastic() to prevent blocking the event loop.
 */
@Component
public class CardRepositoryAdapter implements CardRepositoryPort {

    private final CardJpaRepository jpaRepository;

    public CardRepositoryAdapter(CardJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Mono<List<Card>> findByCustomerId(String customerId) {
        return Mono.fromCallable(() -> jpaRepository.findByCustomerId(customerId))
                .subscribeOn(Schedulers.boundedElastic());
    }
}

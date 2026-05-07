package com.corebank.core.application.port.output;

import com.corebank.core.domain.model.Card;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Output port for card data access.
 */
public interface CardRepositoryPort {

    Mono<List<Card>> findByCustomerId(String customerId);
}

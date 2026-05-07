package com.corebank.core.application.port.output;

import com.corebank.core.domain.model.Balance;
import reactor.core.publisher.Mono;

/**
 * Output port for balance data access.
 */
public interface BalanceRepositoryPort {

    Mono<Balance> findByCustomerId(String customerId);
}

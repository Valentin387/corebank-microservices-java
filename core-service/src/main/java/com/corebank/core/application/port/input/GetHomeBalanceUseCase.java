package com.corebank.core.application.port.input;

import com.corebank.core.domain.model.HomeAggregate;
import reactor.core.publisher.Mono;

/**
 * Input port for the home/balance aggregation use case.
 * Returns a reactive Mono for non-blocking orchestration.
 */
public interface GetHomeBalanceUseCase {

    /**
     * Get aggregated balance, accounts, and cards for a customer.
     *
     * @param customerId the customer identifier
     * @return reactive Mono containing the home aggregate
     */
    Mono<HomeAggregate> getAggregatedBalance(String customerId);
}

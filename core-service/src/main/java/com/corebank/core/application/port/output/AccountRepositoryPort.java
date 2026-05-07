package com.corebank.core.application.port.output;

import com.corebank.core.domain.model.Account;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Output port for account data access.
 */
public interface AccountRepositoryPort {

    Mono<List<Account>> findByCustomerId(String customerId);
}

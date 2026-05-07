package com.corebank.core.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Domain value object aggregating all home page data.
 * Composed from Account, Card, and Balance entities.
 */
@Getter
@Builder
@AllArgsConstructor
public class HomeAggregate {
    private final List<Account> accounts;
    private final List<Card> cards;
    private final Balance balance;
}

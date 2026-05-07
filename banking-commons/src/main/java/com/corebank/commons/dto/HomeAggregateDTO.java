package com.corebank.commons.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Aggregated homepage response combining accounts, cards, and balance.
 * This is the main response body for GET /api/home/balance.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeAggregateDTO {
    private List<AccountDTO> accounts;
    private List<CardDTO> cards;
    private BalanceDTO balance;
}

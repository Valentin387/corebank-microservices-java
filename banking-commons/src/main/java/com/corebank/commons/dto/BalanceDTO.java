package com.corebank.commons.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Balance data transfer object.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceDTO {
    private String customerId;
    private BigDecimal totalBalance;
    private BigDecimal availableBalance;
}

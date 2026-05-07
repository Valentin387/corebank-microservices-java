package com.corebank.commons.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Card data transfer object for homepage aggregation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardDTO {
    private Long id;
    private String cardNumber;
    private String cardType;
    private BigDecimal creditLimit;
    private BigDecimal availableBalance;
}

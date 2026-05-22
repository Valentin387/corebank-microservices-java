package com.corebank.commons.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Domain event: Account balance updated (transaction).
 * Published by core-service when balance changes.
 * Consumed by core-service and potentially other services for audit / projections.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class BalanceUpdatedEvent extends BaseDomainEvent {
    
    @JsonProperty("accountId")
    private Long accountId;
    
    @JsonProperty("accountNumber")
    private String accountNumber;
    
    @JsonProperty("previousBalance")
    private BigDecimal previousBalance;
    
    @JsonProperty("newBalance")
    private BigDecimal newBalance;
    
    @JsonProperty("transactionType")
    private String transactionType;

    public BalanceUpdatedEvent(String customerId, String custIdentNum, String custIdentType,
                               Long accountId, String accountNumber, BigDecimal previousBalance,
                               BigDecimal newBalance, String transactionType) {
        setAggregateId(customerId);
        setCustIdentNum(custIdentNum);
        setCustIdentType(custIdentType);
        setEventId(generateEventId());
        setTimestamp(java.time.Instant.now());
        setAggregateType("Balance");
        setEventType("BalanceUpdatedEvent");
        
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.previousBalance = previousBalance;
        this.newBalance = newBalance;
        this.transactionType = transactionType;
    }
}

package com.corebank.commons.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Domain event: New account created for a customer.
 * Published by core-service when account creation occurs.
 * Consumed by core-service itself to update read models and projections.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AccountCreatedEvent extends BaseDomainEvent {
    
    @JsonProperty("accountNumber")
    private String accountNumber;
    
    @JsonProperty("accountType")
    private String accountType;
    
    @JsonProperty("initialBalance")
    private BigDecimal initialBalance;
    
    @JsonProperty("currency")
    private String currency;

    public AccountCreatedEvent(String customerId, String custIdentNum, String custIdentType,
                               String accountNumber, String accountType, BigDecimal initialBalance, String currency) {
        setAggregateId(customerId);
        setCustIdentNum(custIdentNum);
        setCustIdentType(custIdentType);
        setEventId(generateEventId());
        setTimestamp(java.time.Instant.now());
        setAggregateType("Account");
        setEventType("AccountCreatedEvent");
        
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.initialBalance = initialBalance;
        this.currency = currency;
    }
}

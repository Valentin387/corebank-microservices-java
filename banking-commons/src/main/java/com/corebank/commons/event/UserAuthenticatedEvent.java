package com.corebank.commons.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Domain event: User successfully authenticated.
 * Published by auth-service after successful login.
 * Consumed by core-service to update customer session / read models.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthenticatedEvent extends BaseDomainEvent {
    
    @JsonProperty("username")
    private String username;
    
    @JsonProperty("jwtToken")
    private String jwtToken;
    
    @JsonProperty("sessionId")
    private String sessionId;

    public UserAuthenticatedEvent(String aggregateId, String custIdentNum, String custIdentType,
                                  String username, String jwtToken, String sessionId) {
        setAggregateId(aggregateId);
        setCustIdentNum(custIdentNum);
        setCustIdentType(custIdentType);
        setEventId(generateEventId());
        setTimestamp(java.time.Instant.now());
        setAggregateType("User");
        setEventType("UserAuthenticatedEvent");
        
        this.username = username;
        this.jwtToken = jwtToken;
        this.sessionId = sessionId;
    }
}

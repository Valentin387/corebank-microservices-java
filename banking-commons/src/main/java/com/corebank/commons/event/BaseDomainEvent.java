package com.corebank.commons.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all domain events in the CoreBank microservices ecosystem.
 * Provides standardized metadata: eventId, aggregateId, timestamp, traceId, and banking headers.
 */
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseDomainEvent {
    
    @JsonProperty("eventId")
    private String eventId;
    
    @JsonProperty("aggregateId")
    private String aggregateId;
    
    @JsonProperty("aggregateType")
    private String aggregateType;
    
    @JsonProperty("eventType")
    private String eventType;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("traceId")
    private String traceId;
    
    @JsonProperty("custIdentNum")
    private String custIdentNum;
    
    @JsonProperty("custIdentType")
    private String custIdentType;

    /**
     * Generate a new event ID (UUID-based idempotency key).
     */
    public static String generateEventId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Ensure all events have necessary fields populated before publishing.
     */
    public void validate() {
        if (eventId == null) {
            this.eventId = generateEventId();
        }
        if (timestamp == null) {
            this.timestamp = Instant.now();
        }
        if (aggregateType == null) {
            this.aggregateType = this.getClass().getSimpleName();
        }
        if (eventType == null) {
            this.eventType = this.getClass().getSimpleName();
        }
    }
}

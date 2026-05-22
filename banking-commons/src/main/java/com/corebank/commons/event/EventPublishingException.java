package com.corebank.commons.event;

/**
 * Thrown when a domain event cannot be published to the event bus (Kafka).
 */
public class EventPublishingException extends RuntimeException {
    
    public EventPublishingException(String message) {
        super(message);
    }
    
    public EventPublishingException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.corebank.commons.event;

/**
 * Output port for publishing domain events.
 * Implemented by infrastructure adapters (e.g., KafkaEventPublisherAdapter).
 * 
 * Hexagonal Architecture: Application layer depends on this port;
 * infrastructure layer provides the concrete implementation.
 */
public interface EventPublisherPort {
    
    /**
     * Publish a domain event asynchronously.
     * 
     * @param event the domain event to publish
     * @throws EventPublishingException if publishing fails
     */
    void publish(BaseDomainEvent event) throws EventPublishingException;
    
    /**
     * Publish a domain event to a specific Kafka topic.
     * 
     * @param topic the target Kafka topic name
     * @param event the domain event to publish
     * @throws EventPublishingException if publishing fails
     */
    void publishToTopic(String topic, BaseDomainEvent event) throws EventPublishingException;
}

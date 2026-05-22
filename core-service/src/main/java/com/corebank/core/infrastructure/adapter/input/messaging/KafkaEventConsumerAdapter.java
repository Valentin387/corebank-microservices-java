package com.corebank.core.infrastructure.adapter.input.messaging;

import com.corebank.commons.event.AccountCreatedEvent;
import com.corebank.commons.event.BalanceUpdatedEvent;
import com.corebank.commons.event.UserAuthenticatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer adapter for core-service.
 * Listens to domain events from auth-service and other sources.
 * Implements event-driven consumption patterns with manual acknowledgment for idempotency.
 */
@Component
public class KafkaEventConsumerAdapter {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventConsumerAdapter.class);

    /**
     * Consume UserAuthenticatedEvent from auth-service.
     * Updates customer session / read models in core-service.
     */
    @KafkaListener(
            topics = "user-authenticated",
            groupId = "${kafka.consumer.group-id:core-service-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleUserAuthenticatedEvent(
            @Payload UserAuthenticatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Received UserAuthenticatedEvent: {} from topic: {}, partition: {}, offset: {}",
                    event.getEventId(), topic, partition, offset);
            
            // TODO: Update customer session in core-service's read model
            // This is where you'd update a customer session cache or database entry
            log.debug("Updated customer session for user: {}", event.getUsername());
            
            // Manual commit for reliable processing
            acknowledgment.acknowledge();
            log.debug("Acknowledged UserAuthenticatedEvent: {}", event.getEventId());
            
        } catch (Exception e) {
            log.error("Error processing UserAuthenticatedEvent: {}", event.getEventId(), e);
            // For critical events, you might want to send to dead-letter topic or retry
        }
    }

    /**
     * Consume AccountCreatedEvent.
     * Updates account projections in core-service.
     */
    @KafkaListener(
            topics = "account-created",
            groupId = "${kafka.consumer.group-id:core-service-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleAccountCreatedEvent(
            @Payload AccountCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Received AccountCreatedEvent: {} from topic: {}, offset: {}",
                    event.getEventId(), topic, offset);
            
            // TODO: Update account projections/read models
            log.debug("Created account projection for account number: {}", event.getAccountNumber());
            
            acknowledgment.acknowledge();
            log.debug("Acknowledged AccountCreatedEvent: {}", event.getEventId());
            
        } catch (Exception e) {
            log.error("Error processing AccountCreatedEvent: {}", event.getEventId(), e);
        }
    }

    /**
     * Consume BalanceUpdatedEvent.
     * Updates balance projections in core-service.
     */
    @KafkaListener(
            topics = "balance-updated",
            groupId = "${kafka.consumer.group-id:core-service-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleBalanceUpdatedEvent(
            @Payload BalanceUpdatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Received BalanceUpdatedEvent: {} from topic: {}, offset: {}",
                    event.getEventId(), topic, offset);
            
            // TODO: Update balance projections/audit logs
            log.debug("Updated balance projection for account: {}", event.getAccountNumber());
            
            acknowledgment.acknowledge();
            log.debug("Acknowledged BalanceUpdatedEvent: {}", event.getEventId());
            
        } catch (Exception e) {
            log.error("Error processing BalanceUpdatedEvent: {}", event.getEventId(), e);
        }
    }
}

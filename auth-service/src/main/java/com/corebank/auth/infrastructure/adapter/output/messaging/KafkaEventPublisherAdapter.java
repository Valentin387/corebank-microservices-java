package com.corebank.auth.infrastructure.adapter.output.messaging;

import com.corebank.commons.event.BaseDomainEvent;
import com.corebank.commons.event.EventPublisherPort;
import com.corebank.commons.event.EventPublishingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * Kafka adapter implementing the EventPublisherPort.
 * Publishes domain events to Kafka topics asynchronously.
 * Supports idempotent event delivery via eventId as key.
 */
@Component
public class KafkaEventPublisherAdapter implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisherAdapter.class);

    private final KafkaTemplate<String, BaseDomainEvent> kafkaTemplate;

    public KafkaEventPublisherAdapter(KafkaTemplate<String, BaseDomainEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(BaseDomainEvent event) throws EventPublishingException {
        // Determine topic from event type
        String topic = mapEventTypeToTopic(event);
        publishToTopic(topic, event);
    }

    @Override
    public void publishToTopic(String topic, BaseDomainEvent event) throws EventPublishingException {
        try {
            // Validate event before publishing
            event.validate();

            // Use eventId as Kafka message key for idempotency
            // Send directly with topic, key, value
            kafkaTemplate.send(topic, event.getEventId(), event).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish event {} to topic {}", event.getEventId(), topic, ex);
                } else {
                    log.info("Successfully published event {} (type: {}) to topic {} with offset {}",
                            event.getEventId(), event.getEventType(), topic,
                            result.getRecordMetadata().offset());
                }
            });

        } catch (Exception e) {
            String errorMsg = "Error publishing event " + event.getEventId() + " to topic " + topic;
            log.error(errorMsg, e);
            throw new EventPublishingException(errorMsg, e);
        }
    }

    /**
     * Map event type to Kafka topic name.
     * Convention: UserAuthenticatedEvent → user-authenticated
     */
    private String mapEventTypeToTopic(BaseDomainEvent event) {
        String eventType = event.getEventType();
        if (eventType == null) {
            eventType = event.getClass().getSimpleName();
        }
        // Convert CamelCase to kebab-case
        return eventType
                .replaceAll("Event$", "")
                .replaceAll("([a-z])([A-Z])", "$1-$2")
                .toLowerCase();
    }
}

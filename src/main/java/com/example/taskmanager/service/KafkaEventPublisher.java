package com.example.taskmanager.service;

import com.example.taskmanager.dto.TicketCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String ticketCreatedTopic;

    public KafkaEventPublisher(
            @Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.ticket-created:ticket-created-topic}") String ticketCreatedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.ticketCreatedTopic = ticketCreatedTopic;
    }

    public void publishTicketCreated(TicketCreatedEvent event) {
        if (kafkaTemplate == null) {
            log.warn("KafkaTemplate not available. Event dispatch simulated for ticket: {}", event.ticketCode());
            return;
        }

        try {
            log.info("Emitting Kafka event to topic [{}] for ticket: {}", ticketCreatedTopic, event.ticketCode());
            kafkaTemplate.send(ticketCreatedTopic, event.ticketCode(), event);
        } catch (Exception ex) {
            log.error("Failed to publish Kafka ticket event {}: {}", event.ticketCode(), ex.getMessage());
        }
    }
}

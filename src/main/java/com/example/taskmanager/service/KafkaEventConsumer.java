package com.example.taskmanager.service;

import com.example.taskmanager.client.AuthServiceClient;
import com.example.taskmanager.dto.TicketCreatedEvent;
import com.example.taskmanager.dto.UserDto;
import com.example.taskmanager.dto.UserRegisteredEvent;
import com.example.taskmanager.entity.Task;
import com.example.taskmanager.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(KafkaEventConsumer.class);

    private final TaskRepository taskRepository;
    private final AuthServiceClient authServiceClient;

    public KafkaEventConsumer(
            TaskRepository taskRepository,
            @Autowired(required = false) AuthServiceClient authServiceClient) {
        this.taskRepository = taskRepository;
        this.authServiceClient = authServiceClient;
    }

    /**
     * Kafka Listener for User Registered Events from auth-service
     */
    @KafkaListener(
            topics = "${app.kafka.topics.user-registered:user-registered-topic}",
            groupId = "${spring.kafka.consumer.group-id:go-seek-backend-group}"
    )
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received Kafka [UserRegisteredEvent] for user: {} ({})", event.name(), event.email());

        // Synchronous Inter-Microservice Feign Call: verify user profile directly from auth-service
        if (authServiceClient != null) {
            try {
                log.info("Calling auth-service via OpenFeign for email: {}", event.email());
                UserDto userProfile = authServiceClient.getUserByEmail(event.email());
                log.info("OpenFeign call successful! User confirmed in auth-service: ID={}, Name={}",
                        userProfile.id(), userProfile.name());
            } catch (Exception ex) {
                log.warn("Feign call to auth-service was non-critical or timed out: {}", ex.getMessage());
            }
        }

        // Initialize a welcome task for the user in app_db
        try {
            Task welcomeTask = new Task();
            welcomeTask.setOwnerEmail(event.email());
            welcomeTask.setTitle("Welcome to Go-Seek & Systems Suite!");
            welcomeTask.setDescription("Your account has been provisioned across microservices. Explore hardware telemetry diagnostics and report any issues.");
            welcomeTask.setCompleted(false);
            taskRepository.save(welcomeTask);
            log.info("Provisioned default starter task in app_db for new user: {}", event.email());
        } catch (Exception ex) {
            log.error("Could not create initial task in app_db: {}", ex.getMessage());
        }
    }

    /**
     * Kafka Listener for Ticket Created Events
     */
    @KafkaListener(
            topics = "${app.kafka.topics.ticket-created:ticket-created-topic}",
            groupId = "${spring.kafka.consumer.group-id:go-seek-backend-group}"
    )
    public void handleTicketCreated(TicketCreatedEvent event) {
        log.info("Received Kafka [TicketCreatedEvent]: Code={}, App={}, Title={}",
                event.ticketCode(), event.appSource(), event.title());
    }
}

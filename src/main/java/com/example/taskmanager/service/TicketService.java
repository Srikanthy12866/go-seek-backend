package com.example.taskmanager.service;

import com.example.taskmanager.dto.TicketCreatedEvent;
import com.example.taskmanager.dto.TicketRequest;
import com.example.taskmanager.dto.TicketResponse;
import com.example.taskmanager.entity.Ticket;
import com.example.taskmanager.repository.TicketRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class TicketService {
    private final TicketRepository ticketRepository;
    private final EmailService emailService;
    private final KafkaEventPublisher kafkaEventPublisher;

    public TicketService(
            TicketRepository ticketRepository,
            EmailService emailService,
            KafkaEventPublisher kafkaEventPublisher) {
        this.ticketRepository = ticketRepository;
        this.emailService = emailService;
        this.kafkaEventPublisher = kafkaEventPublisher;
    }

    @Transactional
    public TicketResponse createTicket(TicketRequest request) {
        Ticket ticket = new Ticket();
        ticket.setAppSource(request.appSource().trim());
        ticket.setTitle(request.title().trim());
        ticket.setDescription(request.description().trim());
        ticket.setUserEmail(request.userEmail().trim().toLowerCase());
        ticket.setUserName(request.userName() != null ? request.userName().trim() : "Anonymous");
        ticket.setPriority(request.priority() != null && !request.priority().isBlank()
                ? request.priority().trim().toUpperCase() : "MEDIUM");
        ticket.setStatus("OPEN");
        ticket.setSystemInfo(request.systemInfo());

        Ticket saved = ticketRepository.save(ticket);

        // 1. Emit Kafka event for asynchronous distributed processing
        try {
            kafkaEventPublisher.publishTicketCreated(new TicketCreatedEvent(
                    saved.getTicketCode(),
                    saved.getAppSource(),
                    saved.getTitle(),
                    saved.getUserEmail(),
                    saved.getPriority(),
                    Instant.now()
            ));
        } catch (Exception ex) {
            // Non-blocking
        }

        // 2. Dispatch email notification to srikanthy12866@gmail.com
        try {
            emailService.sendTicketNotification(saved);
        } catch (Exception e) {
            // Non-blocking
        }

        return TicketResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getAllTickets(String source) {
        List<Ticket> list;
        if (source != null && !source.isBlank()) {
            list = ticketRepository.findByAppSourceOrderByCreatedAtDesc(source.trim());
        } else {
            list = ticketRepository.findAllByOrderByCreatedAtDesc();
        }
        return list.stream().map(TicketResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse getByCode(String code) {
        return ticketRepository.findByTicketCode(code)
                .map(TicketResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
    }

    @Transactional
    public TicketResponse updateStatus(Long id, String status) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        ticket.setStatus(status.toUpperCase());
        return TicketResponse.from(ticketRepository.save(ticket));
    }
}

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
        String rawPriority = request.priority();
        String priority = (rawPriority != null && List.of("LOW", "MEDIUM", "HIGH", "CRITICAL").contains(rawPriority.trim().toUpperCase()))
                ? rawPriority.trim().toUpperCase() : "MEDIUM";
        ticket.setPriority(priority);
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

        // 2. Dispatch email notification to configured recipient
        try {
            emailService.sendTicketNotification(saved);
        } catch (Exception e) {
            // Non-blocking
        }

        return TicketResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getTicketsForUser(String userEmail, String source, boolean isAdmin) {
        if (userEmail == null && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required to view tickets");
        }

        List<Ticket> list;
        if (isAdmin) {
            if (source != null && !source.isBlank()) {
                list = ticketRepository.findByAppSourceOrderByCreatedAtDesc(source.trim());
            } else {
                list = ticketRepository.findAllByOrderByCreatedAtDesc();
            }
        } else {
            // Tenant isolation: Users can only see tickets associated with their authenticated email
            list = ticketRepository.findByUserEmailOrderByCreatedAtDesc(userEmail.toLowerCase());
            if (source != null && !source.isBlank()) {
                list = list.stream().filter(t -> source.equalsIgnoreCase(t.getAppSource())).toList();
            }
        }
        return list.stream().map(TicketResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse getByCode(String code, String callerEmail, boolean isAdmin) {
        Ticket ticket = ticketRepository.findByTicketCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        if (!isAdmin && (callerEmail == null || !callerEmail.equalsIgnoreCase(ticket.getUserEmail()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this ticket");
        }

        return TicketResponse.from(ticket);
    }

    @Transactional
    public TicketResponse updateStatus(Long id, String status, String callerEmail, boolean isAdmin) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        // Broken Object-Level Authorization (BOLA) Defense
        if (!isAdmin && (callerEmail == null || !callerEmail.equalsIgnoreCase(ticket.getUserEmail()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Unauthorized to update this ticket");
        }

        String normalizedStatus = status != null ? status.trim().toUpperCase() : "OPEN";
        if (!List.of("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED").contains(normalizedStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status. Allowed values: OPEN, IN_PROGRESS, RESOLVED, CLOSED");
        }

        ticket.setStatus(normalizedStatus);
        return TicketResponse.from(ticketRepository.save(ticket));
    }
}

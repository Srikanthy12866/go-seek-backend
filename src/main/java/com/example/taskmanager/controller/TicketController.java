package com.example.taskmanager.controller;

import com.example.taskmanager.dto.TicketRequest;
import com.example.taskmanager.dto.TicketResponse;
import com.example.taskmanager.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse submitTicket(@Valid @RequestBody TicketRequest request) {
        return ticketService.createTicket(request);
    }

    @GetMapping
    public List<TicketResponse> listTickets(
            @RequestParam(required = false) String source,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ticketService.getTicketsForUser(email, source, isAdmin);
    }

    @GetMapping("/track/{code}")
    public TicketResponse trackTicket(@PathVariable String code) {
        return ticketService.getByCodePublic(code);
    }

    @GetMapping("/my")
    public List<TicketResponse> getMyTickets(@RequestParam(required = false) String email) {
        if (email == null || email.isBlank()) {
            return List.of();
        }
        return ticketService.getTicketsByEmailPublic(email);
    }

    @GetMapping("/{code}")
    public TicketResponse getTicket(
            @PathVariable String code,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ticketService.getByCode(code, email, isAdmin);
    }

    @PatchMapping("/{id}/status")
    public TicketResponse updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String status = body.getOrDefault("status", "OPEN");
        String email = authentication != null ? authentication.getName() : null;
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ticketService.updateStatus(id, status, email, isAdmin);
    }
}

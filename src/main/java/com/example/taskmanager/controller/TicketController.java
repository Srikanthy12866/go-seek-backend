package com.example.taskmanager.controller;

import com.example.taskmanager.dto.TicketRequest;
import com.example.taskmanager.dto.TicketResponse;
import com.example.taskmanager.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
            @RequestParam(required = false) String source) {
        return ticketService.getAllTickets(source);
    }

    @GetMapping("/{code}")
    public TicketResponse getTicket(@PathVariable String code) {
        return ticketService.getByCode(code);
    }

    @PatchMapping("/{id}/status")
    public TicketResponse updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.getOrDefault("status", "OPEN");
        return ticketService.updateStatus(id, status);
    }
}

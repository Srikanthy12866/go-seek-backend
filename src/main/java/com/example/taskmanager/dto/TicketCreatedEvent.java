package com.example.taskmanager.dto;

import java.time.Instant;

public record TicketCreatedEvent(
        String ticketCode,
        String appSource,
        String title,
        String userEmail,
        String priority,
        Instant timestamp
) {}

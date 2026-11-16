package com.example.taskmanager.dto;

import com.example.taskmanager.entity.Ticket;
import java.time.Instant;

public record TicketResponse(
        Long id,
        String ticketCode,
        String appSource,
        String title,
        String description,
        String userName,
        String userEmail,
        String priority,
        String status,
        String systemInfo,
        Instant createdAt,
        Instant updatedAt
) {
    public static TicketResponse from(Ticket t) {
        return new TicketResponse(
                t.getId(),
                t.getTicketCode(),
                t.getAppSource(),
                t.getTitle(),
                t.getDescription(),
                t.getUserName(),
                t.getUserEmail(),
                t.getPriority(),
                t.getStatus(),
                t.getSystemInfo(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}

package com.example.taskmanager.dto;

import com.example.taskmanager.entity.ContactMessage;
import java.time.Instant;

public record ContactResponse(
        Long id,
        String name,
        String email,
        String subject,
        String message,
        Instant createdAt,
        String status
) {
    public static ContactResponse from(ContactMessage m, String status) {
        return new ContactResponse(
                m.getId(),
                m.getName(),
                m.getEmail(),
                m.getSubject(),
                m.getMessage(),
                m.getCreatedAt(),
                status
        );
    }
}

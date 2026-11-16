package com.example.taskmanager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TicketRequest(
        @NotBlank(message = "App source is required (e.g. 'Go-Seek' or 'Portfolio')")
        String appSource,

        @NotBlank(message = "Issue title is required")
        @Size(max = 200, message = "Title cannot exceed 200 characters")
        String title,

        @NotBlank(message = "Description of the issue is required")
        String description,

        @NotBlank(message = "Your email is required")
        @Email(message = "A valid email is required")
        String userEmail,

        String userName,
        String priority, // LOW, MEDIUM, HIGH, CRITICAL
        String systemInfo // Diagnostics context
) {}

package com.example.taskmanager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 120, message = "Name cannot exceed 120 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "A valid email is required")
        @Size(max = 190, message = "Email cannot exceed 190 characters")
        String email,

        @Size(max = 200, message = "Subject cannot exceed 200 characters")
        String subject,

        @NotBlank(message = "Message cannot be empty")
        String message
) {}

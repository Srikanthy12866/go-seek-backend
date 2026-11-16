package com.example.taskmanager.dto;

import java.time.Instant;

public record UserRegisteredEvent(
        Long userId,
        String name,
        String email,
        Instant timestamp
) {}

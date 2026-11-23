package com.example.taskmanager.controller;

import com.example.taskmanager.service.InterServiceHealthCheckService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @Value("${app.notification.recipient-email:srikanthy12866@gmail.com}")
    private String notifyEmail;

    private final InterServiceHealthCheckService healthCheckService;

    public HealthController(InterServiceHealthCheckService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "go-seek-backend",
                "port", 8082,
                "database", "app_db",
                "notificationTarget", notifyEmail,
                "peerHealth", Map.of(
                        "target", "auth-service",
                        "status", healthCheckService.getPeerStatus(),
                        "lastChecked", healthCheckService.getLastCheckedTime()
                ),
                "timestamp", System.currentTimeMillis()
        );
    }
}

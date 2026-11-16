package com.example.taskmanager.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tickets", indexes = {
        @Index(name = "idx_tickets_source", columnList = "app_source"),
        @Index(name = "idx_tickets_status", columnList = "status"),
        @Index(name = "idx_tickets_email", columnList = "user_email")
})
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_code", nullable = false, unique = true, length = 32)
    private String ticketCode;

    @Column(name = "app_source", nullable = false, length = 60)
    private String appSource; // "Go-Seek" or "Portfolio"

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "user_name", length = 120)
    private String userName;

    @Column(name = "user_email", nullable = false, length = 190)
    private String userEmail;

    @Column(length = 40)
    private String priority = "MEDIUM"; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(length = 40)
    private String status = "OPEN"; // OPEN, IN_PROGRESS, RESOLVED, CLOSED

    @Column(columnDefinition = "TEXT")
    private String systemInfo; // Diagnostics telemetry data if from Go-Seek

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (ticketCode == null || ticketCode.isBlank()) {
            ticketCode = "TCK-" + (System.currentTimeMillis() % 1000000);
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTicketCode() { return ticketCode; }
    public void setTicketCode(String ticketCode) { this.ticketCode = ticketCode; }
    public String getAppSource() { return appSource; }
    public void setAppSource(String appSource) { this.appSource = appSource; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSystemInfo() { return systemInfo; }
    public void setSystemInfo(String systemInfo) { this.systemInfo = systemInfo; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

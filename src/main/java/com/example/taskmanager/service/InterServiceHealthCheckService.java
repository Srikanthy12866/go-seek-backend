package com.example.taskmanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Periodically polls the peer microservice (auth-service) health endpoint
 * every 10 minutes to verify connectivity and maintain inter-service telemetry.
 */
@Service
public class InterServiceHealthCheckService {
    private static final Logger log = LoggerFactory.getLogger(InterServiceHealthCheckService.class);

    @Value("${app.services.auth-url:http://localhost:8081}")
    private String authServiceUrl;

    @Value("${app.health-check.enabled:true}")
    private boolean enabled;

    private final HttpClient httpClient;
    private final AtomicReference<String> peerStatus = new AtomicReference<>("UNKNOWN");
    private final AtomicReference<String> lastCheckedTime = new AtomicReference<>("NEVER");

    public InterServiceHealthCheckService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Executes every 10 minutes (600,000 ms) with an initial 10-second delay after startup
     */
    @Scheduled(fixedRateString = "${app.health-check.interval-ms:600000}", initialDelay = 10000)
    public void checkAuthServiceHealth() {
        if (!enabled) {
            return;
        }

        String targetUrl = authServiceUrl.replaceAll("/+$", "") + "/api/health";
        String now = Instant.now().toString();
        lastCheckedTime.set(now);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(5))
                    .header("User-Agent", "go-seek-backend-health-checker/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                peerStatus.set("UP");
                log.info("[HealthCheck] Peer service (auth-service) is UP at {}. HTTP 200 OK: {}", targetUrl, response.body());
            } else {
                peerStatus.set("DEGRADED");
                log.warn("[HealthCheck] Peer service (auth-service) at {} responded with HTTP {}", targetUrl, response.statusCode());
            }
        } catch (Exception ex) {
            peerStatus.set("DOWN");
            log.warn("[HealthCheck] Unable to reach peer service (auth-service) at {}: {}", targetUrl, ex.getMessage());
        }
    }

    public String getPeerStatus() {
        return peerStatus.get();
    }

    public String getLastCheckedTime() {
        return lastCheckedTime.get();
    }
}

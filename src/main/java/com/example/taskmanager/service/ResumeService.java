package com.example.taskmanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@Service
public class ResumeService {
    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);

    @Value("${app.resume.file-path:}")
    private String configuredFilePath;

    @Value("${app.resume.remote-url:}")
    private String configuredRemoteUrl;

    public byte[] getResumeBytes() throws Exception {
        // 1. Try local configured file path
        if (configuredFilePath != null && !configuredFilePath.isBlank()) {
            Path path = Path.of(configuredFilePath);
            if (Files.exists(path) && Files.isReadable(path)) {
                log.info("Serving resume from configured file path: {}", configuredFilePath);
                return Files.readAllBytes(path);
            }
        }

        // 2. Try configured remote server-side proxy URL (Google Drive / Supabase storage)
        if (configuredRemoteUrl != null && !configuredRemoteUrl.isBlank()) {
            try {
                log.info("Fetching resume server-side from remote source...");
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(6))
                        .followRedirects(HttpClient.Redirect.ALWAYS)
                        .build();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(configuredRemoteUrl))
                        .GET()
                        .build();
                HttpResponse<byte[]> res = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (res.statusCode() == 200 && res.body().length > 0) {
                    return res.body();
                }
            } catch (Exception ex) {
                log.warn("Could not retrieve remote resume ({}), falling back to bundled resume", ex.getMessage());
            }
        }

        // 3. Bundled resource fallback
        ClassPathResource resource = new ClassPathResource("resume.pdf");
        if (resource.exists()) {
            try (InputStream in = resource.getInputStream()) {
                return in.readAllBytes();
            }
        }

        throw new IllegalStateException("Resume file is currently unavailable.");
    }
}

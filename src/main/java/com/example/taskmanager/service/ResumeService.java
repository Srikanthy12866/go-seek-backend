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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResumeService {
    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);

    public record ResumeFile(
            byte[] bytes,
            String filename,
            String source,
            long lastModified,
            long size
    ) {}

    @Value("${app.resume.folder-path:C:/Users/admin/OneDrive/Documents/Resume}")
    private String configuredFolderPath;

    @Value("${app.resume.file-path:}")
    private String configuredFilePath;

    @Value("${app.resume.remote-url:}")
    private String configuredRemoteUrl;

    public String getConfiguredFolderPath() {
        return configuredFolderPath;
    }

    /**
     * Retrieve the latest resume file dynamically:
     * 1. Scans configured folder location (picks latest/matching PDF without requiring manual renaming).
     * 2. Checks local default resume folder if exists.
     * 3. Checks direct file path if configured.
     * 4. Server-side proxy remote URL fallback.
     * 5. Bundled classpath fallback.
     */
    public ResumeFile getResume() throws Exception {
        // 1. Try configured folder location
        if (configuredFolderPath != null && !configuredFolderPath.isBlank()) {
            ResumeFile file = findLatestResumeInFolder(Path.of(configuredFolderPath));
            if (file != null) {
                return file;
            }
        }

        // 2. Fallback to default local Resume folder if present on the system
        Path defaultLocalPath = Path.of("C:/Users/admin/OneDrive/Documents/Resume");
        if (Files.exists(defaultLocalPath) && Files.isDirectory(defaultLocalPath)) {
            ResumeFile file = findLatestResumeInFolder(defaultLocalPath);
            if (file != null) {
                return file;
            }
        }

        // Also check relative ./resumes folder
        Path relativeResumes = Path.of("./resumes");
        if (Files.exists(relativeResumes) && Files.isDirectory(relativeResumes)) {
            ResumeFile file = findLatestResumeInFolder(relativeResumes);
            if (file != null) {
                return file;
            }
        }

        // 3. Try configured specific file path
        if (configuredFilePath != null && !configuredFilePath.isBlank()) {
            Path path = Path.of(configuredFilePath);
            if (Files.exists(path) && Files.isReadable(path)) {
                log.info("Serving resume from configured file path: {}", configuredFilePath);
                return new ResumeFile(
                        Files.readAllBytes(path),
                        path.getFileName().toString(),
                        "file:" + path.toAbsolutePath(),
                        Files.getLastModifiedTime(path).toMillis(),
                        Files.size(path)
                );
            }
        }

        // 4. Try configured remote server-side proxy URL (Google Drive / cloud storage)
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
                    return new ResumeFile(
                            res.body(),
                            "Yepalagunta_Srikanth_Resume.pdf",
                            "remote-url",
                            System.currentTimeMillis(),
                            res.body().length
                    );
                }
            } catch (Exception ex) {
                log.warn("Could not retrieve remote resume ({}), falling back to bundled resume", ex.getMessage());
            }
        }

        // 5. Bundled resource fallback
        ClassPathResource resource = new ClassPathResource("resume.pdf");
        if (resource.exists()) {
            try (InputStream in = resource.getInputStream()) {
                byte[] bytes = in.readAllBytes();
                return new ResumeFile(
                        bytes,
                        "Yepalagunta_Srikanth_Resume.pdf",
                        "bundled-classpath",
                        System.currentTimeMillis(),
                        bytes.length
                );
            }
        }

        throw new IllegalStateException("Resume file is currently unavailable.");
    }

    /**
     * Backward compatibility method for byte-only retrieval.
     */
    public byte[] getResumeBytes() throws Exception {
        return getResume().bytes();
    }

    /**
     * Scans a directory for the latest PDF resume file.
     * Prioritizes files matching 'srikanth' or 'resume' and picks the latest modified file.
     */
    private ResumeFile findLatestResumeInFolder(Path folderPath) {
        try {
            if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
                return null;
            }

            try (var stream = Files.list(folderPath)) {
                List<Path> pdfFiles = stream
                        .filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".pdf"))
                        .collect(Collectors.toList());

                if (pdfFiles.isEmpty()) {
                    log.info("No PDF files found in folder location: {}", folderPath);
                    return null;
                }

                // Comparator to sort descending by last modified time (newest first)
                Comparator<Path> byLastModifiedDesc = Comparator.comparingLong((Path p) -> {
                    try {
                        return Files.getLastModifiedTime(p).toMillis();
                    } catch (Exception e) {
                        return 0L;
                    }
                }).reversed();

                // Check for files matching srikanth or resume
                List<Path> matchingNames = pdfFiles.stream()
                        .filter(p -> {
                            String name = p.getFileName().toString().toLowerCase();
                            return name.contains("srikanth") || name.contains("resume");
                        })
                        .sorted(byLastModifiedDesc)
                        .collect(Collectors.toList());

                Path chosenPath = !matchingNames.isEmpty() ? matchingNames.get(0) : pdfFiles.stream().sorted(byLastModifiedDesc).findFirst().orElse(null);

                if (chosenPath != null && Files.isReadable(chosenPath)) {
                    log.info("Captured resume dynamically from folder: {} -> {} (size: {} bytes)",
                            folderPath, chosenPath.getFileName(), Files.size(chosenPath));
                    return new ResumeFile(
                            Files.readAllBytes(chosenPath),
                            chosenPath.getFileName().toString(),
                            "folder:" + chosenPath.toAbsolutePath(),
                            Files.getLastModifiedTime(chosenPath).toMillis(),
                            Files.size(chosenPath)
                    );
                }
            }
        } catch (Exception ex) {
            log.warn("Error scanning resume folder {}: {}", folderPath, ex.getMessage());
        }
        return null;
    }
}

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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    private record GDriveEntry(String id, String href, String title) {}

    // Google Drive Folder ID provided by user: https://drive.google.com/drive/folders/14_DQNf3XbcSpzc-UD_H8fHoIClZeQkNq
    @Value("${app.resume.gdrive-folder-id:14_DQNf3XbcSpzc-UD_H8fHoIClZeQkNq}")
    private String gdriveFolderId;

    @Value("${app.resume.gdrive-api-key:}")
    private String gdriveApiKey;

    @Value("${app.resume.folder-path:C:/Users/admin/OneDrive/Documents/Resume}")
    private String configuredFolderPath;

    @Value("${app.resume.file-path:}")
    private String configuredFilePath;

    @Value("${app.resume.remote-url:https://drive.google.com/uc?export=download&id=1h8OlcwXugTswgIhwxdsFdTDhfVWW8Hk8}")
    private String configuredRemoteUrl;

    // In-memory cache with 5-minute TTL to ensure fast responses and avoid rate limiting
    private ResumeFile cachedResume = null;
    private long cacheExpiryTime = 0L;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    public String getGdriveFolderId() {
        return gdriveFolderId;
    }

    public String getConfiguredFolderPath() {
        return configuredFolderPath;
    }

    public ResumeFile getResume() throws Exception {
        long now = System.currentTimeMillis();
        if (cachedResume != null && now < cacheExpiryTime) {
            return cachedResume;
        }

        ResumeFile fetched = fetchLatestResume();
        if (fetched != null) {
            this.cachedResume = fetched;
            this.cacheExpiryTime = now + CACHE_TTL_MS;
            return fetched;
        }

        if (cachedResume != null) {
            return cachedResume;
        }

        throw new IllegalStateException("Resume file is currently unavailable.");
    }

    public byte[] getResumeBytes() throws Exception {
        return getResume().bytes();
    }

    private ResumeFile fetchLatestResume() {
        // 1. Google Drive Folder auto-capture (Primary)
        if (gdriveFolderId != null && !gdriveFolderId.isBlank()) {
            try {
                ResumeFile fromGdriveFolder = fetchFromGoogleDriveFolder(gdriveFolderId.trim());
                if (fromGdriveFolder != null) {
                    return fromGdriveFolder;
                }
            } catch (Exception ex) {
                log.warn("Could not fetch from Google Drive folder {}: {}", gdriveFolderId, ex.getMessage());
            }
        }

        // 2. Direct remote URL fallback (e.g. direct file ID from Google Drive)
        if (configuredRemoteUrl != null && !configuredRemoteUrl.isBlank()) {
            try {
                ResumeFile fromUrl = downloadFileFromUrl(configuredRemoteUrl, "Yepalagunta_Srikanth_Resume.pdf", "remote-url");
                if (fromUrl != null) {
                    return fromUrl;
                }
            } catch (Exception ex) {
                log.warn("Could not retrieve remote resume from direct URL: {}", ex.getMessage());
            }
        }

        // 3. Configured local folder location (if running on host machine)
        if (configuredFolderPath != null && !configuredFolderPath.isBlank()) {
            ResumeFile file = findLatestResumeInFolder(Path.of(configuredFolderPath));
            if (file != null) {
                return file;
            }
        }

        // 4. Default local folder if present
        Path defaultLocalPath = Path.of("C:/Users/admin/OneDrive/Documents/Resume");
        if (Files.exists(defaultLocalPath) && Files.isDirectory(defaultLocalPath)) {
            ResumeFile file = findLatestResumeInFolder(defaultLocalPath);
            if (file != null) {
                return file;
            }
        }

        // 5. Configured direct file path
        if (configuredFilePath != null && !configuredFilePath.isBlank()) {
            Path path = Path.of(configuredFilePath);
            if (Files.exists(path) && Files.isReadable(path)) {
                try {
                    return new ResumeFile(
                            Files.readAllBytes(path),
                            path.getFileName().toString(),
                            "file:" + path.toAbsolutePath(),
                            Files.getLastModifiedTime(path).toMillis(),
                            Files.size(path)
                    );
                } catch (Exception ignored) {}
            }
        }

        // 6. Bundled classpath fallback
        try {
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
        } catch (Exception ignored) {}

        return null;
    }

    /**
     * Resolves and downloads the latest resume from a Google Drive folder.
     */
    private ResumeFile fetchFromGoogleDriveFolder(String folderId) {
        log.info("Scanning Google Drive folder: {}", folderId);

        // A. If an API key is configured, use Google Drive API v3
        if (gdriveApiKey != null && !gdriveApiKey.isBlank()) {
            try {
                String apiUrl = "https://www.googleapis.com/drive/v3/files?q='" + folderId + "'+in+parents+and+trashed=false"
                        + "&orderBy=modifiedTime+desc&fields=files(id,name,mimeType,modifiedTime,size)&key=" + gdriveApiKey;
                HttpClient client = createHttpClient();
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(apiUrl)).GET().build();
                HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() == 200) {
                    Pattern idPattern = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
                    Pattern namePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
                    Matcher idMatcher = idPattern.matcher(res.body());
                    Matcher nameMatcher = namePattern.matcher(res.body());
                    if (idMatcher.find() && nameMatcher.find()) {
                        String fileId = idMatcher.group(1);
                        String filename = nameMatcher.group(1);
                        log.info("Google Drive API discovered file in folder: {} ({})", filename, fileId);
                        String downloadUrl = "https://drive.google.com/uc?export=download&id=" + fileId;
                        return downloadFileFromUrl(downloadUrl, filename, "gdrive-folder-api:" + folderId);
                    }
                }
            } catch (Exception ex) {
                log.warn("Google Drive API query failed: {}", ex.getMessage());
            }
        }

        // B. Fetch from Google Drive folder web view
        try {
            HttpClient client = createHttpClient();
            String embeddedUrl = "https://drive.google.com/embeddedfolderview?id=" + folderId;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(embeddedUrl))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .GET()
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                String html = res.body();

                // Parse entries from embedded folder view
                Pattern entryPattern = Pattern.compile("<div class=\"flip-entry\" id=\"entry-([^\"]+)\"[\\s\\S]*?<a href=\"([^\"]+)\"[\\s\\S]*?<div class=\"flip-entry-title\">([^<]+)</div>");
                Matcher entryMatcher = entryPattern.matcher(html);
                List<GDriveEntry> entries = new ArrayList<>();
                while (entryMatcher.find()) {
                    entries.add(new GDriveEntry(
                            entryMatcher.group(1).trim(),
                            entryMatcher.group(2).trim(),
                            entryMatcher.group(3).trim()
                    ));
                }

                if (!entries.isEmpty()) {
                    log.info("Discovered {} file(s) in Google Drive folder {}: {}",
                            entries.size(), folderId, entries.stream().map(GDriveEntry::title).collect(Collectors.joining(", ")));

                    // Prioritize PDF files
                    List<GDriveEntry> pdfs = entries.stream()
                            .filter(e -> e.title().toLowerCase().endsWith(".pdf"))
                            .toList();

                    GDriveEntry chosen = !pdfs.isEmpty() ? pdfs.get(0) : entries.get(0);

                    String downloadUrl;
                    if (chosen.href().contains("/document/d/")) {
                        downloadUrl = "https://docs.google.com/document/d/" + chosen.id() + "/export?format=pdf";
                    } else {
                        downloadUrl = "https://drive.google.com/uc?export=download&id=" + chosen.id();
                    }

                    log.info("Capturing resume: {} ({})", chosen.title(), downloadUrl);
                    ResumeFile file = downloadFileFromUrl(downloadUrl, chosen.title(), "gdrive-folder:" + folderId);
                    if (file != null) {
                        return file;
                    }
                }

                // Fallback ID regex scanner
                Pattern filePattern = Pattern.compile("(?:file\\/d\\/|data-id=[\"'])([a-zA-Z0-9_-]{25,50})");
                Matcher m = filePattern.matcher(html);
                Set<String> candidateFileIds = new LinkedHashSet<>();
                while (m.find()) {
                    candidateFileIds.add(m.group(1));
                }

                for (String fileId : candidateFileIds) {
                    String downloadUrl = "https://drive.google.com/uc?export=download&id=" + fileId;
                    ResumeFile file = downloadFileFromUrl(downloadUrl, "Yepalagunta_Srikanth_Resume.pdf", "gdrive-folder:" + folderId);
                    if (file != null) {
                        return file;
                    }
                }
            } else {
                log.warn("Google Drive folder {} returned HTTP {}.", folderId, res.statusCode());
            }
        } catch (Exception ex) {
            log.warn("Error parsing Google Drive folder: {}", ex.getMessage());
        }

        return null;
    }

    private boolean isSafeRemoteUrl(String url) {
        try {
            URI uri = URI.create(url);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                return false;
            }
            String host = uri.getHost();
            if (host == null || host.isBlank() || host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1") || host.equals("169.254.169.254")) {
                return false;
            }
            java.net.InetAddress[] addresses = java.net.InetAddress.getAllByName(host);
            for (java.net.InetAddress addr : addresses) {
                if (addr.isLoopbackAddress() || addr.isLinkLocalAddress() || addr.isSiteLocalAddress() || addr.isAnyLocalAddress()) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private ResumeFile downloadFileFromUrl(String url, String fallbackFilename, String source) {
        if (!isSafeRemoteUrl(url)) {
            log.warn("Blocked potentially unsafe / SSRF remote URL request: {}", url);
            return null;
        }
        try {
            HttpClient client = createHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .GET()
                    .build();

            HttpResponse<byte[]> res = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (res.statusCode() == 200 && res.body().length > 0) {
                String filename = fallbackFilename;
                Optional<String> disposition = res.headers().firstValue("content-disposition");
                if (disposition.isPresent() && disposition.get().contains("filename=")) {
                    Matcher m = Pattern.compile("filename[^;=\\n]*=((['\"]).*?\\2|[^;\\n]*)").matcher(disposition.get());
                    if (m.find() && m.group(1) != null) {
                        filename = m.group(1).replace("\"", "").replace("'", "").trim();
                    }
                }
                log.info("Successfully retrieved resume from {}: {} ({} bytes)", source, filename, res.body().length);
                return new ResumeFile(
                        res.body(),
                        filename,
                        source,
                        System.currentTimeMillis(),
                        res.body().length
                );
            }
        } catch (Exception ex) {
            log.warn("Failed to download from URL {}: {}", url, ex.getMessage());
        }
        return null;
    }

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
                    return null;
                }

                Comparator<Path> byLastModifiedDesc = Comparator.comparingLong((Path p) -> {
                    try {
                        return Files.getLastModifiedTime(p).toMillis();
                    } catch (Exception e) {
                        return 0L;
                    }
                }).reversed();

                List<Path> matchingNames = pdfFiles.stream()
                        .filter(p -> {
                            String name = p.getFileName().toString().toLowerCase();
                            return name.contains("srikanth") || name.contains("resume");
                        })
                        .sorted(byLastModifiedDesc)
                        .collect(Collectors.toList());

                Path chosenPath = !matchingNames.isEmpty() ? matchingNames.get(0) : pdfFiles.stream().sorted(byLastModifiedDesc).findFirst().orElse(null);

                if (chosenPath != null && Files.isReadable(chosenPath)) {
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

    private HttpClient createHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }
}

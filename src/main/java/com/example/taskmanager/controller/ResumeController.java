package com.example.taskmanager.controller;

import com.example.taskmanager.service.ResumeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    private final ResumeService resumeService;
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB limit
    private static final byte[] PDF_MAGIC_BYTES = new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadResume() {
        try {
            ResumeService.ResumeFile resumeFile = resumeService.getResume();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", resumeFile.filename());
            headers.setContentLength(resumeFile.bytes().length);
            return new ResponseEntity<>(resumeFile.bytes(), headers, HttpStatus.OK);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume could not be retrieved: " + ex.getMessage());
        }
    }

    @GetMapping("/info")
    public Map<String, Object> resumeInfo() {
        try {
            ResumeService.ResumeFile resumeFile = resumeService.getResume();
            return Map.of(
                    "status", "AVAILABLE",
                    "filename", resumeFile.filename(),
                    "contentType", "application/pdf",
                    "source", resumeFile.source(),
                    "sizeBytes", resumeFile.size(),
                    "lastModified", resumeFile.lastModified(),
                    "gdriveFolderId", resumeService.getGdriveFolderId(),
                    "folderLocation", resumeService.getConfiguredFolderPath(),
                    "downloadUrl", "/api/resume/download",
                    "secured", true
            );
        } catch (Exception ex) {
            return Map.of(
                    "status", "UNAVAILABLE",
                    "error", "Resume information unavailable",
                    "gdriveFolderId", resumeService.getGdriveFolderId(),
                    "folderLocation", resumeService.getConfiguredFolderPath(),
                    "secured", true
            );
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadResume(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required to upload resume"));
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Uploaded file is empty"));
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest().body(Map.of("error", "File size exceeds the 5MB maximum limit"));
        }

        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid filename"));
            }

            // Path Traversal Mitigation: Strip path separators and verify extension
            Path sanitizedPath = Path.of(originalFilename).getFileName();
            String safeBaseName = sanitizedPath.toString();
            if (safeBaseName.contains("..") || !safeBaseName.toLowerCase().endsWith(".pdf")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Only valid .pdf files are permitted"));
            }

            // Magic Bytes Validation: Ensure header starts with %PDF
            try (InputStream is = file.getInputStream()) {
                byte[] header = new byte[4];
                int read = is.read(header);
                if (read < 4 || !Arrays.equals(header, PDF_MAGIC_BYTES)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "File content does not match a valid PDF document"));
                }
            }

            String targetFolder = resumeService.getConfiguredFolderPath();
            Path targetDir = (targetFolder != null && !targetFolder.isBlank())
                    ? Path.of(targetFolder).toAbsolutePath().normalize()
                    : Path.of("./resumes").toAbsolutePath().normalize();

            Files.createDirectories(targetDir);

            // Server-side secure filename generation
            String safeStoredFilename = UUID.randomUUID() + "_" + safeBaseName.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path destination = targetDir.resolve(safeStoredFilename).normalize();

            // Guard against traversal beyond target directory
            if (!destination.startsWith(targetDir)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid destination path"));
            }

            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            return ResponseEntity.ok(Map.of(
                    "message", "Resume uploaded successfully into secure storage",
                    "filename", safeStoredFilename,
                    "sizeBytes", file.getSize()
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process resume upload securely"));
        }
    }
}

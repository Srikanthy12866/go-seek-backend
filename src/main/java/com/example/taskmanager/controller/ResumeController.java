package com.example.taskmanager.controller;

import com.example.taskmanager.service.ResumeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    private final ResumeService resumeService;

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
                    "error", ex.getMessage(),
                    "gdriveFolderId", resumeService.getGdriveFolderId(),
                    "folderLocation", resumeService.getConfiguredFolderPath(),
                    "secured", true
            );
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadResume(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Uploaded file is empty"));
        }
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Only PDF resumes are supported"));
            }

            String targetFolder = resumeService.getConfiguredFolderPath();
            Path targetDir = (targetFolder != null && !targetFolder.isBlank())
                    ? Path.of(targetFolder)
                    : Path.of("./resumes");

            Files.createDirectories(targetDir);
            Path destination = targetDir.resolve(originalFilename);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            return ResponseEntity.ok(Map.of(
                    "message", "Resume uploaded successfully into folder location",
                    "savedPath", destination.toAbsolutePath().toString(),
                    "filename", originalFilename,
                    "sizeBytes", file.getSize()
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to save resume: " + ex.getMessage()));
        }
    }
}

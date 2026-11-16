package com.example.taskmanager.controller;

import com.example.taskmanager.service.ResumeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
            byte[] fileBytes = resumeService.getResumeBytes();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "Yepalagunta_Srikanth_Resume.pdf");
            headers.setContentLength(fileBytes.length);
            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume could not be retrieved: " + ex.getMessage());
        }
    }

    @GetMapping("/info")
    public Map<String, Object> resumeInfo() {
        return Map.of(
                "filename", "Yepalagunta_Srikanth_Resume.pdf",
                "contentType", "application/pdf",
                "downloadUrl", "/api/resume/download",
                "secured", true
        );
    }
}

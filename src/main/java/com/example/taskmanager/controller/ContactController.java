package com.example.taskmanager.controller;

import com.example.taskmanager.dto.ContactRequest;
import com.example.taskmanager.dto.ContactResponse;
import com.example.taskmanager.entity.ContactMessage;
import com.example.taskmanager.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contact")
public class ContactController {
    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContactResponse submitContact(@Valid @RequestBody ContactRequest request) {
        return contactService.submitContact(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ContactMessage> listMessages() {
        return contactService.getAllMessages();
    }
}

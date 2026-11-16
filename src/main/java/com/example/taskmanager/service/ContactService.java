package com.example.taskmanager.service;

import com.example.taskmanager.dto.ContactRequest;
import com.example.taskmanager.dto.ContactResponse;
import com.example.taskmanager.entity.ContactMessage;
import com.example.taskmanager.repository.ContactMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContactService {
    private final ContactMessageRepository contactMessageRepository;
    private final EmailService emailService;

    public ContactService(ContactMessageRepository contactMessageRepository, EmailService emailService) {
        this.contactMessageRepository = contactMessageRepository;
        this.emailService = emailService;
    }

    @Transactional
    public ContactResponse submitContact(ContactRequest request) {
        ContactMessage msg = new ContactMessage();
        msg.setName(request.name().trim());
        msg.setEmail(request.email().trim().toLowerCase());
        msg.setSubject(request.subject() != null ? request.subject().trim() : "Portfolio Contact");
        msg.setMessage(request.message().trim());

        ContactMessage saved = contactMessageRepository.save(msg);

        // Async notify via email to srikanthy12866@gmail.com
        try {
            emailService.sendContactNotification(saved);
        } catch (Exception e) {
            // Non-blocking
        }

        return ContactResponse.from(saved, "RECEIVED");
    }

    @Transactional(readOnly = true)
    public List<ContactMessage> getAllMessages() {
        return contactMessageRepository.findAllByOrderByCreatedAtDesc();
    }
}

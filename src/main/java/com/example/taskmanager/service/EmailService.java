package com.example.taskmanager.service;

import com.example.taskmanager.entity.ContactMessage;
import com.example.taskmanager.entity.Ticket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String recipientEmail;
    private final String fromEmail;
    private final String mailPassword;

    public EmailService(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${app.notification.recipient-email:srikanthy12866@gmail.com}") String recipientEmail,
            @Value("${spring.mail.username:srikanthy12866@gmail.com}") String fromEmail,
            @Value("${spring.mail.password:}") String mailPassword) {
        this.mailSender = mailSender;
        this.recipientEmail = recipientEmail;
        this.fromEmail = fromEmail;
        this.mailPassword = mailPassword != null ? mailPassword.trim() : "";
    }

    public boolean isSmtpConfigured() {
        return mailSender != null && !mailPassword.isBlank();
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    @Async
    public void sendContactNotification(ContactMessage msg) {
        String subject = "[Portfolio Contact] New message from " + msg.getName();
        String body = String.format(
                """
                You have received a new contact inquiry from your portfolio!

                From: %s
                Email: %s
                Subject: %s
                Received At: %s

                Message:
                --------------------------------------------------
                %s
                --------------------------------------------------
                """,
                msg.getName(),
                msg.getEmail(),
                msg.getSubject() != null ? msg.getSubject() : "General Inquiry",
                msg.getCreatedAt(),
                msg.getMessage()
        );

        deliverEmail(subject, body, msg.getEmail());
    }

    @Async
    public void sendTicketNotification(Ticket ticket) {
        String subject = String.format("[%s Issue Ticket %s] %s",
                ticket.getAppSource(), ticket.getTicketCode(), ticket.getTitle());

        String body = String.format(
                """
                A new issue ticket has been reported!

                Ticket Code: %s
                App Source:  %s
                Title:       %s
                Priority:    %s
                Reported By: %s (%s)
                Reported At: %s

                Description:
                --------------------------------------------------
                %s
                --------------------------------------------------

                System / Diagnostics Context:
                %s
                """,
                ticket.getTicketCode(),
                ticket.getAppSource(),
                ticket.getTitle(),
                ticket.getPriority(),
                ticket.getUserName() != null ? ticket.getUserName() : "Anonymous",
                ticket.getUserEmail(),
                ticket.getCreatedAt(),
                ticket.getDescription(),
                ticket.getSystemInfo() != null && !ticket.getSystemInfo().isBlank()
                        ? ticket.getSystemInfo() : "None provided"
        );

        deliverEmail(subject, body, ticket.getUserEmail());
    }

    private void deliverEmail(String subject, String content, String replyTo) {
        log.info("Preparing to send email to [{}]: {}", recipientEmail, subject);

        if (mailSender == null || mailPassword.isBlank()) {
            log.warn("[SMTP Notification Pending] MAIL_PASSWORD is not set or JavaMailSender is missing. " +
                    "To deliver emails to {}, please configure a 16-character Google App Password in the MAIL_PASSWORD environment variable for account {}. " +
                    "Logging notification payload locally instead:", recipientEmail, fromEmail);
            log.info("Email Payload:\nSubject: {}\n{}", subject, content);
            return;
        }

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(recipientEmail);
            mailMessage.setFrom(fromEmail);
            if (replyTo != null && !replyTo.isBlank()) {
                mailMessage.setReplyTo(replyTo);
            }
            mailMessage.setSubject(subject);
            mailMessage.setText(content);

            mailSender.send(mailMessage);
            log.info("Email successfully dispatched via SMTP to {}", recipientEmail);
        } catch (Exception ex) {
            log.error("Failed to send email via SMTP ({}), logging content locally:", ex.getMessage());
            log.info("Email Payload:\nSubject: {}\n{}", subject, content);
        }
    }
}

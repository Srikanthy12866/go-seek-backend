package com.example.taskmanager.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Service to verify inbound webhook signatures using HMAC-SHA256
 * and constant-time comparison to prevent timing attacks.
 */
@Service
public class WebhookSecurityService {

    private final String webhookSecret;

    public WebhookSecurityService(@Value("${app.webhook.secret:sec_whk_0987654321fedcba_prod}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public boolean verifySignature(String payload, String receivedSignature) {
        if (payload == null || receivedSignature == null || receivedSignature.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = HexFormat.of().formatHex(hash);

            // Constant-time comparison to prevent timing attacks
            return MessageDigest.isEqual(
                    calculatedSignature.getBytes(StandardCharsets.UTF_8),
                    receivedSignature.trim().getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }
}

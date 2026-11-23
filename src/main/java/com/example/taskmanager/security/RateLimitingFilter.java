package com.example.taskmanager.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory sliding window rate limiting filter for go-seek-backend.
 * Protects ticket creation, contact submissions, and resume downloads from DoS.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private record WindowBucket(long windowStartTimeMs, AtomicInteger requestCount) {}

    private final Map<String, WindowBucket> requestBuckets = new ConcurrentHashMap<>();
    private static final long WINDOW_DURATION_MS = 60_000L;
    private static final int SENSITIVE_LIMIT_PER_MINUTE = 20;
    private static final int GENERAL_LIMIT_PER_MINUTE = 150;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        String clientIp = getClientIp(request);

        boolean isSensitive = ("POST".equalsIgnoreCase(method) && (path.startsWith("/api/tickets") || path.startsWith("/api/contact")))
                || path.startsWith("/api/resume/upload");
        int maxAllowed = isSensitive ? SENSITIVE_LIMIT_PER_MINUTE : GENERAL_LIMIT_PER_MINUTE;

        String bucketKey = clientIp + ":" + (isSensitive ? "sensitive" : "general");
        long now = System.currentTimeMillis();

        WindowBucket bucket = requestBuckets.compute(bucketKey, (key, existing) -> {
            if (existing == null || (now - existing.windowStartTimeMs) > WINDOW_DURATION_MS) {
                return new WindowBucket(now, new AtomicInteger(1));
            }
            existing.requestCount.incrementAndGet();
            return existing;
        });

        if (bucket.requestCount.get() > maxAllowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            response.getWriter().write("""
                {
                    "status": 429,
                    "error": "Too Many Requests",
                    "message": "Rate limit exceeded. Please wait before retrying."
                }
                """);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

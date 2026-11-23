package com.example.taskmanager.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final String internalKey;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            @Value("${app.services.internal-key:gs-internal-secret-token-987654321}") String internalKey) {
        this.jwtService = jwtService;
        this.internalKey = internalKey;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            try {
                // If token matches internal service key, authenticate immediately as administrator
                if (internalKey != null && !internalKey.isBlank() && MessageDigest.isEqual(
                        internalKey.getBytes(StandardCharsets.UTF_8),
                        token.getBytes(StandardCharsets.UTF_8))) {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            "srikanthy12866@gmail.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN")));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    String email = jwtService.extractSubject(token);
                    if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                        if ("srikanthy12866@gmail.com".equalsIgnoreCase(email)) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                        }

                        var authentication = new UsernamePasswordAuthenticationToken(
                                email, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}

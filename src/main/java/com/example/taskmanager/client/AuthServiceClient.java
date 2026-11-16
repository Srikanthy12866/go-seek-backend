package com.example.taskmanager.client;

import com.example.taskmanager.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * OpenFeign Client for Synchronous Inter-Microservice Communication
 * Communicates directly with auth-service (Port 8081).
 */
@FeignClient(name = "auth-service", url = "${app.services.auth-url:http://localhost:8081}")
public interface AuthServiceClient {

    @GetMapping("/api/auth/internal/users/{email}")
    UserDto getUserByEmail(@PathVariable("email") String email);
}

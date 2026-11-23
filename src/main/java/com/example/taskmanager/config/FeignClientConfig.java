package com.example.taskmanager.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign client configuration injecting the shared internal service key
 * for secure microservice-to-microservice authentication.
 */
@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor internalKeyRequestInterceptor(
            @Value("${app.services.internal-key:gs-internal-secret-token-987654321}") String internalKey) {
        return requestTemplate -> requestTemplate.header("X-Internal-Service-Key", internalKey);
    }
}

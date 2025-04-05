package com.example.domain.api.authentication_service.security.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Validated
@Data
public class JWTConfig {
    @NotBlank
    private String secret;

    private long accessExpiration;

    private long refreshExpiration;
}

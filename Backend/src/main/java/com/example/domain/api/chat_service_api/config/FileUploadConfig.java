package com.example.domain.api.chat_service_api.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "file-upload")
@Validated
@Data
public class FileUploadConfig {

    @NotBlank
    private String location;
    @NotBlank
    private String clamavHost;
    @Min(1)
    private int clamavPort;
}
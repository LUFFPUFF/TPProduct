package com.example.domain.api.chat_service_api.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "validation.file-upload")
@Validated
@Data
public class ValidationConfig {

    @NotBlank
    private String maxFileSize;
    @NotEmpty
    private List<String> allowedFileTypes;
}

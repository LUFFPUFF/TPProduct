package com.example.domain.api.ans_api_module.template.config;

import com.example.domain.api.ans_api_module.template.services.TempFileStorageService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class TempFileStorageConfig {

    @Bean
    public TempFileStorageService tempFileStorageService() throws IOException {
        return new TempFileStorageService();
    }
}

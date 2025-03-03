package com.example.domain.api.chat_service_api.config;

import fi.solita.clamav.ClamAVClient;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClamAVCConfigBuilder {

    private final FileUploadConfig fileUploadConfig;

    @Autowired
    public ClamAVCConfigBuilder(FileUploadConfig fileUploadConfig) {
        this.fileUploadConfig = fileUploadConfig;
    }

    @Bean
    public ClamAVClient clamAVClient() {
        return new ClamAVClient(fileUploadConfig.getClamavHost(), fileUploadConfig.getClamavPort());
    }
}

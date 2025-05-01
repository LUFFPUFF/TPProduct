package com.example.domain.api.chat_service_api.integration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Configuration
public class IntegrationConfig {

    @Bean
    public BlockingQueue<Object> incomingMessageQueue() {
        return new LinkedBlockingQueue<>();
    }

    @Bean
    public BlockingQueue<Object> outgoingMessageQueue() {
        return new LinkedBlockingQueue<>();
    }

    @Bean
    public JavaMailSender getJavaMailSender() {
        return new JavaMailSenderImpl();
    }
}

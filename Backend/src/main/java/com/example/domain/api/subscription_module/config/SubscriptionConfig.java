package com.example.domain.api.subscription_module.config;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "subscription")
@Data
public class SubscriptionConfig {
    private BigDecimal price;
}

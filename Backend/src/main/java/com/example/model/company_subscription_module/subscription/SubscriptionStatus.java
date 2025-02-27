package com.example.model.company_subscription_module.subscription;

import lombok.Getter;

@Getter
public enum SubscriptionStatus {

    ACTIVE("Активная"),
    EXPIRED("Закончилась"),
    CANCELLED("Отменена");

    private final String description;

    SubscriptionStatus(String description) {
        this.description = description;
    }
}

package com.example.database.model.integration_module;

import lombok.Getter;

@Getter
public enum IntegrationStatus {

    ACTIVE("Активная"),
    DISABLED("Неактивная"),
    ERROR("Ошибка");

    private final String description;

    IntegrationStatus(String description) {
        this.description = description;
    }
}

package com.example.database.model.crm_module.task;

import lombok.Getter;

@Getter
public enum TaskStatus {

    OPENED("Открыта"),
    CLOSED("Закрыта");

    private final String description;

    TaskStatus(String description) {
        this.description = description;
    }
}

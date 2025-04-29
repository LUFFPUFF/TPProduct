package com.example.database.model.crm_module.task;

import lombok.Getter;

@Getter
public enum TaskPriority {

    LOW("Низкий"),
    MEDIUM("Средний"),
    HIGH("Высокий");

    private final String description;

    TaskPriority(String description) {
        this.description = description;
    }
}

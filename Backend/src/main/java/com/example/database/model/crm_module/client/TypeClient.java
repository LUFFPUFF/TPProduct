package com.example.database.model.crm_module.client;

import lombok.Getter;

@Getter
public enum TypeClient {

    IMPORTANT("Важный"),
    PROBLEMATIC("Проблемный");

    private final String description;

    TypeClient(String description) {
        this.description = description;
    }
}

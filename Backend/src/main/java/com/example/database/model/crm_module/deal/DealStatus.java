package com.example.database.model.crm_module.deal;

import lombok.Getter;

@Getter
public enum DealStatus {

    OPENED("Открыта"),
    CLOSED("Закрыта");

    private final String description;

    DealStatus(String description) {
        this.description = description;
    }
}

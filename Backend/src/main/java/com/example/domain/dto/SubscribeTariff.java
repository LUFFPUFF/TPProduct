package com.example.domain.dto;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public enum SubscribeTariff {
    SOLO(BigDecimal.valueOf(790)),
    TEST(BigDecimal.ZERO),
    DYNAMIC(BigDecimal.valueOf(790));
    private final BigDecimal price;
    SubscribeTariff(BigDecimal price) {
        this.price = price;
    }

}

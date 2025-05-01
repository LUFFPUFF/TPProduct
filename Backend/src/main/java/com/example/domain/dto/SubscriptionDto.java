package com.example.domain.dto;


import com.example.database.model.company_subscription_module.subscription.SubscriptionStatus;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SubscriptionDto {


    private SubscriptionStatus status;

    private float cost;

    private int countOperators;

    private int maxOperators;

    private LocalDateTime startSubscription;

    private LocalDateTime endSubscription;

}

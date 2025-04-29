package com.example.domain.api.subscription_module.service;

import com.example.database.model.company_subscription_module.Company;
import com.example.domain.dto.SubscribeDataDto;
import com.example.domain.dto.SubscriptionDto;
import com.example.domain.dto.SubscriptionPriceReqDto;

import java.math.BigDecimal;

public interface SubscriptionService {
    SubscriptionDto subscribe(SubscribeDataDto subscribeDataDto);
    void cancel();
    SubscriptionDto getSubscription();
    Float countPrice(SubscriptionPriceReqDto subscriptionPriceDto);
    void addOperatorCount(Company company);
    void subtractOperatorCount(Company company);
}

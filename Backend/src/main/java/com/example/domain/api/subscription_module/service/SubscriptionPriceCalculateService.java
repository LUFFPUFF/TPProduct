package com.example.domain.api.subscription_module.service;

import com.example.database.model.company_subscription_module.subscription.Subscription;
import com.example.domain.dto.SubscriptionExtendPriceDto;
import com.example.domain.dto.SubscriptionPriceReqDto;

import java.math.BigDecimal;

public interface SubscriptionPriceCalculateService{
    BigDecimal calculateDiscountMonths(Integer months);
    BigDecimal calculateDiscountPeople(Integer people);
    BigDecimal calculateTotalPrice(SubscriptionPriceReqDto subscriptionPriceReqDto);
    BigDecimal calculateTotalPrice(SubscriptionExtendPriceDto subscriptionPriceReqDto, Subscription subscription);

}

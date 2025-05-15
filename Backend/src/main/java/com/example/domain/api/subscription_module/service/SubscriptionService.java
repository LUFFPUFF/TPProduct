package com.example.domain.api.subscription_module.service;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.domain.dto.*;

import java.math.BigDecimal;

public interface SubscriptionService {
    SubscriptionDto subscribe(SubscribeDataDto subscribeDataDto);
    SubscriptionDto subscribeNewUser(SubscribeDataDto subscribeDataDto, String email);
    SubscriptionDto renewSubscription(SubscribeDataDto subscribeDataDto);
    PriceDto getExtendPrice(SubscriptionExtendPriceDto subscriptionExtendPriceDto);
    SubscriptionDto extendSubscription(SubscribeExtendDataDto subscribeDataDto);
    SubscriptionDto getSubscription();
    PriceDto countPrice(SubscriptionPriceReqDto subscriptionPriceDto);
    void addOperatorCount(Company company);
    void subtractOperatorCount(Company company);
    void cancel();
}

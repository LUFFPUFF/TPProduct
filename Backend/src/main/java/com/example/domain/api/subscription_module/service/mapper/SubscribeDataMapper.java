package com.example.domain.api.subscription_module.service.mapper;

import com.example.database.model.company_subscription_module.subscription.Subscription;
import com.example.database.model.company_subscription_module.subscription.SubscriptionStatus;
import com.example.domain.api.subscription_module.service.SubscriptionPriceCalculateService;
import com.example.domain.api.subscription_module.service.SubscriptionService;
import com.example.domain.dto.SubscribeDataDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor

public class SubscribeDataMapper {

    private final SubscriptionPriceCalculateService subscriptionPriceCalculateService;

    public Subscription toSubscription(SubscribeDataDto dataDto,int renew) {
        Subscription subscription = new Subscription();
        if (renew != 0) {
            subscription.setCountOperators(renew);
        }else {
            subscription.setCountOperators(1);
        }
        subscription.setMaxOperators(dataDto.getPrice().getOperators_count());
        subscription.setCompany(dataDto.getCompany());
        subscription.setStartSubscription(LocalDateTime.now());
        subscription.setCreatedAt(LocalDateTime.now());
        subscription.setUpdatedAt(LocalDateTime.now());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        switch (dataDto.getTariff()){
            case TEST -> {
                subscription.setMaxOperators(1);
                subscription.setEndSubscription(LocalDateTime.now().plusDays(7));
                subscription.setCost(0);
            }
            case SOLO -> {
                subscription.setMaxOperators(1);
                subscription.setEndSubscription(LocalDateTime.now().plusMonths(dataDto.getPrice().getMonths_count()));
                subscription.setCost(subscriptionPriceCalculateService.calculateDiscountMonths(dataDto.getPrice().getMonths_count()).floatValue());
            }
            case DYNAMIC -> {
                subscription.setMaxOperators(dataDto.getPrice().getOperators_count());
                subscription.setEndSubscription(LocalDateTime.now().plusMonths(dataDto.getPrice().getMonths_count()));
                subscription.setCost(subscriptionPriceCalculateService.calculateTotalPrice(dataDto.getPrice()).floatValue());
            }
        }
        return subscription;
    }
}

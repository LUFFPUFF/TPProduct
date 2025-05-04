package com.example.domain.api.subscription_module.service.impl;

import com.example.domain.api.subscription_module.config.SubscriptionConfig;
import com.example.domain.api.subscription_module.service.SubscriptionPriceCalculateService;
import com.example.domain.dto.SubscriptionPriceReqDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class SubscriptionPriceCalculateServiceImpl implements SubscriptionPriceCalculateService {
    private final SubscriptionConfig subscriptionConfig;

    @Override
    public BigDecimal calculateDiscountMonths(Integer months) {
        if (months <= 1) {
            return BigDecimal.ZERO;
        }
        BigDecimal maxDiscount = new BigDecimal("0.25");
        BigDecimal calculated = BigDecimal.valueOf(months).divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP)
                .multiply(maxDiscount);
        return calculated.min(maxDiscount);
    }

    @Override
    public BigDecimal calculateDiscountPeople(Integer people) {
        if (people <= 1) {
            return BigDecimal.ZERO;
        }
        BigDecimal maxDiscount = new BigDecimal("0.25");
        BigDecimal calculated = BigDecimal.valueOf(people).divide(BigDecimal.valueOf(10), 10, RoundingMode.HALF_UP)
                .multiply(maxDiscount);
        return calculated.min(maxDiscount);
    }

    @Override
    public BigDecimal calculateTotalPrice(SubscriptionPriceReqDto subscriptionPriceReqDto) {
        int months= subscriptionPriceReqDto.getMonths_count();
        int people = subscriptionPriceReqDto.getOperators_count();
        BigDecimal discount = (new BigDecimal("0.5").min(calculateDiscountMonths(months).add(calculateDiscountPeople(people))));
        BigDecimal totalPrice = BigDecimal.ONE.subtract(discount);
        totalPrice = totalPrice.multiply(subscriptionConfig.getPrice())
                .multiply(BigDecimal.valueOf(people))
                .multiply(BigDecimal.valueOf(months)).setScale(2, RoundingMode.HALF_DOWN);

        return totalPrice;
    }
}

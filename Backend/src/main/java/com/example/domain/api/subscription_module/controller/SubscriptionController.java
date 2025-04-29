package com.example.domain.api.subscription_module.controller;

import com.example.domain.api.subscription_module.service.SubscriptionService;
import com.example.domain.dto.SubscribeDataDto;
import com.example.domain.dto.SubscriptionDto;
import com.example.domain.dto.SubscriptionPriceReqDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/subscription")
@RequiredArgsConstructor
@Validated
public class SubscriptionController {
    private final SubscriptionService subscribeService;

    @PostMapping("/subscribe")
    public ResponseEntity<SubscriptionDto> subscribe(@RequestBody @Validated SubscribeDataDto subscribeDataDto) {
        return ResponseEntity.ok(subscribeService.subscribe(subscribeDataDto));
    }

    //TODO: Продление, обновление подписки
    @GetMapping("/get")
    public ResponseEntity<SubscriptionDto> getSubscription() {
        return ResponseEntity.ok(subscribeService.getSubscription());
    }

    @PostMapping("/cancel")
    public Boolean cancelSubscription() {
        subscribeService.cancel();
        return true;
    }

    @GetMapping("/price")
    public ResponseEntity<Float> getSubscriptionPrice(@RequestBody @Validated SubscriptionPriceReqDto subscriptionPriceDto) {
        return ResponseEntity.ok(subscribeService.countPrice(subscriptionPriceDto));
    }

}

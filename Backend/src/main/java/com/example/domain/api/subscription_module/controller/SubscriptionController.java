package com.example.domain.api.subscription_module.controller;

import com.example.domain.api.subscription_module.service.SubscriptionService;
import com.example.domain.dto.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/subscription")
@RequiredArgsConstructor
@Validated
public class SubscriptionController {
    private final SubscriptionService subscribeService;

    @PostMapping("/subscribe")
    public ResponseEntity<SubscriptionDto> subscribe(@RequestBody @Validated SubscribeDataDto subscribeDataDto) {
        return ResponseEntity.ok(subscribeService.subscribe(subscribeDataDto));
    }

    @PostMapping("/extend")
    public ResponseEntity<SubscriptionDto> extendSubscription(@RequestBody @Validated SubscribeExtendDataDto subscribeDataDto) {
        return ResponseEntity.ok(subscribeService.extendSubscription(subscribeDataDto));
    }

    @GetMapping("/get")
    public ResponseEntity<SubscriptionDto> getSubscription() {
        SubscriptionDto subscriptionDto = subscribeService.getSubscription();
        System.out.println("---------------------------------------" +
                "" +
                "" +subscriptionDto.getEndSubscription() +
                "" +
                "-----------------------------------------------------");
        return ResponseEntity.ok(subscriptionDto);
    }

    @PostMapping("/cancel")
    public Boolean cancelSubscription() {
        subscribeService.cancel();
        return true;
    }
    @GetMapping("/price/extend")
    public ResponseEntity<PriceDto> extendPriceSubscription(
            @RequestParam @Min(1) @Max(240) Integer months_count,
            @RequestParam @Min(0) @Max(1000) Integer operators_count) {
        SubscriptionExtendPriceDto subscriptionExtendPriceDto = SubscriptionExtendPriceDto.builder()
                .operators_count(operators_count)
                .months_count(months_count).build();
        return ResponseEntity.ok(subscribeService.getExtendPrice(subscriptionExtendPriceDto));
    }
    @GetMapping("/price")
    public ResponseEntity<PriceDto> getSubscriptionPrice(
            @RequestParam @Min(1) @Max(240) Integer months_count,
            @RequestParam @Min(1) @Max(1000) Integer operators_count) {
        SubscriptionPriceReqDto subscriptionPriceDto = SubscriptionPriceReqDto.builder()
                .months_count(months_count)
                .operators_count(operators_count)
                .build();

        return ResponseEntity.ok(subscribeService.countPrice(subscriptionPriceDto));
    }


}

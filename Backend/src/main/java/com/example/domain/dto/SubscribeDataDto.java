package com.example.domain.dto;

import com.example.database.model.company_subscription_module.company.Company;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
public class SubscribeDataDto {
    @NotNull
    @Valid
    SubscriptionPriceReqDto price;
    @NotNull
    private SubscribeTariff tariff;

    private Company company;
}

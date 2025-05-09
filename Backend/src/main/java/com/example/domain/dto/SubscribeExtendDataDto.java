package com.example.domain.dto;

import com.example.database.model.company_subscription_module.company.Company;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscribeExtendDataDto {
    @NotNull
    @Valid
    SubscriptionExtendPriceDto price;
    private Company company;
}

package com.example.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;


@Builder
@Data
public class SubscriptionExtendPriceDto {
    @Min(value = 0, message = "Минимум 0 операторов")
    @Max(value = 1000,message = "Максимум 1000 операторов")
    @NotNull
    Integer operators_count;
    @Min(value = 1, message = "Минимум 1 месяц")
    @Max(value = 240, message = "Максимум 240 месяцев")
    @NotNull
    Integer months_count;
}

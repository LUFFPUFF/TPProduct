package com.example.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenDto {
    @NotNull
    String refresh_token;
    @NotNull
    String access_token;
}

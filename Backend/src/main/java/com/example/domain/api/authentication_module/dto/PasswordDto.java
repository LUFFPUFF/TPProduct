package com.example.domain.api.authentication_module.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PasswordDto {
    @NotNull
    @Size(min = 6)
    String password;
}

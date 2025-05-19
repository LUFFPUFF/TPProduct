package com.example.domain.api.authentication_module.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordCodeDto {
    @NotNull
    @Size(min = 6, max = 6)
    String code;
    @NotNull
    @Size(min = 6)
    String newPassword;
    @Email
    String email;
}

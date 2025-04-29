package com.example.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RegistrationDto {
    @NotNull(message = "Email не может быть пустым")
    @Email(message = "Некорректный email пользователя")
    @Size(max = 50)
    private String email;

    @NotNull
    private String password;

    private String fullName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

package com.example.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RegistrationDto {
    @NotNull(message = "Имя не может быть пустым")
    @Size(max = 50)
    private String fullName;

    @NotNull(message = "Email не может быть пустым")
    @Email(message = "Некорректный email пользователя")
    @Size(max = 50)
    @Pattern(regexp = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$",message = "Некорректный email пользователя")
    private String email;

    @NotNull
    private String password;

    @NotNull
    private LocalDateTime createdAt;

    @NotNull
    private LocalDateTime updatedAt;
}

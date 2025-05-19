package com.example.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
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
    @Size(min = 6)
    private String password;

    private String fullName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
    public RegistrationDto(){};
    public RegistrationDto(String email, String password, String fullName,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

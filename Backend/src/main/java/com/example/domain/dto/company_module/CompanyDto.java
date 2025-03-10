package com.example.domain.dto.company_module;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CompanyDto {
    private Integer id;

    @NotNull(message = "Имя компании не может быть пустым")
    @Size(max = 50, message = "Имя компании не должно превышать 50 символов")
    private String name;

    @NotNull(message = "Контакты компании не могут быть пустыми")
    @Email(message = "Некорректный email компании")
    @Size(max = 50, message = "Контакты компании не должны превышать 50 символов")
    private String contactEmail;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<UserDto> userDtos;
}

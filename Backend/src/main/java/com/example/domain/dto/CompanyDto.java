package com.example.domain.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder

@Getter @Setter
@ToString
public class CompanyDto {

    private Integer id;

    @NotNull(message = "Имя компании не может быть пустым")
    @Size(max = 50, message = "Имя компании не должно превышать 50 символов")
    private String name;

    @NotNull(message = "Контакты компании не могут быть пустыми")
    @Email(message = "Некорректный email компании")
    @Size(max = 50, message = "Контакты компании не должны превышать 50 символов")
    private String contactEmail;

    private String companyDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CompanyDto() {
    }

    public CompanyDto(Integer id, String name, String contactEmail, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.contactEmail = contactEmail;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

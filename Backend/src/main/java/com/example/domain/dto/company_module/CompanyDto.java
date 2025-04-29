package com.example.domain.dto.company_module;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;


@Getter @Setter
@ToString
@Builder
public class CompanyDto {

    private Integer id;

    private String name;

    private String contactEmail;

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

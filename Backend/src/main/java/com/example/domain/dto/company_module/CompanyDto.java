package com.example.domain.dto.company_module;


import lombok.*;

import java.time.LocalDateTime;


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

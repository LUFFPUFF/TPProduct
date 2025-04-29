package com.example.domain.dto.company_module;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleDto {
    private Integer id;

    @Size(max = 50)
    private String name;

    @Size(max = 255)
    private String description;
}

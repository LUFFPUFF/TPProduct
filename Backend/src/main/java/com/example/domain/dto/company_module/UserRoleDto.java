package com.example.domain.dto.company_module;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserRoleDto {

    @NotNull(message = "User ID не может быть пустым")
    private Integer userId;

    @NotNull(message = "Role ID не может быть пустым")
    private Integer roleId;
}

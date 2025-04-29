package com.example.domain.dto;

import com.example.database.model.company_subscription_module.user_roles.user.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserRoleDto {

    @NotNull(message = "User ID не может быть пустым")
    private Integer userId;

    @NotNull(message = "Role ID не может быть пустым")
    private Role role;
}

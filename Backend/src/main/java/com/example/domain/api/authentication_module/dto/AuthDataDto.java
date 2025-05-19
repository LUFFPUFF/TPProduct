package com.example.domain.api.authentication_module.dto;

import com.example.database.model.company_subscription_module.user_roles.user.Role;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AuthDataDto {
    String email;
    List<String> roles;
}

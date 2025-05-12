package com.example.domain.security;

import com.example.database.model.company_subscription_module.user_roles.user.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext {
    private Integer userId;
    private String email;
    private Role role;
    private Integer companyId;
}

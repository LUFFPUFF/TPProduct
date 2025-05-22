package com.example.domain.dto;

import com.example.database.model.company_subscription_module.user_roles.user.Role;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MemberDto {
    String fullName;
    String email;
    List<Role> roles;
}

package com.example.domain.api.company_module.dto;

import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.domain.dto.EmailDto;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberRoleReqDto {
    EmailDto email;
    Role role;
}

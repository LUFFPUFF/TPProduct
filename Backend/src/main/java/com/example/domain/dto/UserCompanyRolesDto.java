package com.example.domain.dto;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Optional;

@Data
@Builder
public class UserCompanyRolesDto {
    private User user;
    private Company company;
    private List<Role> userRoles;

    public UserCompanyRolesDto(User user, Company company, List<Role> roles) {
        this.user = user;
        this.company = company;
        this.userRoles = roles;
    }
}

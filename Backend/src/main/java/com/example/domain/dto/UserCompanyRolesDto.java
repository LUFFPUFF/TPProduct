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
    User user;
    Optional<Company> company;
    List<Role> userRoles;
    UserCompanyRolesDto(User user, Company company) {
        this.user = user;
        this.company = Optional.ofNullable(company);
    }

}

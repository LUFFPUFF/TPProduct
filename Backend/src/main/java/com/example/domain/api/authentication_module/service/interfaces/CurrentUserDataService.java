package com.example.domain.api.authentication_module.service.interfaces;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.model.company_subscription_module.user_roles.user.User;

import java.util.List;

public interface CurrentUserDataService {
    User getUser();
    User getUser(String email);
    Company getUserCompany();
    List<Role> getRoleList();
    boolean hasRole(Role role);
    String getUserEmail();
}

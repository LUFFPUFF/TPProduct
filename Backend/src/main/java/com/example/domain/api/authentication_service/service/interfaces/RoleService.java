package com.example.domain.api.authentication_service.service.interfaces;

import com.example.database.model.company_subscription_module.user_roles.user.Role;

public interface RoleService {
    boolean addRole(String user_email,Role role);
}

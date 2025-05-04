package com.example.domain.api.authentication_module.service.interfaces;

import com.example.database.model.company_subscription_module.user_roles.user.Role;

import java.util.List;

public interface RoleService {
    boolean addRole(String user_email,Role role);
    boolean removeRole(String user_email,Role role);
    List<Role> getUserRoles(String email);
}

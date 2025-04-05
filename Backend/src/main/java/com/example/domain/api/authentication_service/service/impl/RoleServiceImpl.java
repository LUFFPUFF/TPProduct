package com.example.domain.api.authentication_service.service.impl;

import com.example.database.model.company_subscription_module.user_roles.UserRole;
import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.database.repository.company_subscription_module.UserRoleRepository;
import com.example.domain.api.authentication_service.exception_handler_auth.NoFoundUserException;
import com.example.domain.api.authentication_service.service.interfaces.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService  {
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;

    @Override

    public boolean addRole(String userEmail, Role role) {
        return userRepository.findByEmail(userEmail).map(user -> {
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            userRoleRepository.save(userRole);
            return true;
        }).orElseThrow(NoFoundUserException::new);
    }
}

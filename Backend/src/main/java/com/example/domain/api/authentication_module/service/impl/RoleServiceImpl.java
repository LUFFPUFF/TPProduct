package com.example.domain.api.authentication_module.service.impl;

import com.example.database.model.company_subscription_module.user_roles.UserRole;
import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.database.repository.company_subscription_module.UserRoleRepository;
import com.example.domain.api.authentication_module.cache.AuthCacheService;
import com.example.domain.api.authentication_module.exception_handler_auth.NotFoundUserException;
import com.example.domain.api.authentication_module.service.interfaces.RoleService;
import com.example.domain.api.statistics_module.aop.annotation.Counter;
import com.example.domain.api.statistics_module.aop.annotation.MeteredOperation;
import com.example.domain.api.statistics_module.aop.annotation.Tag;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService  {
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final AuthCacheService authCacheService;

    @Override
    @Transactional
    @MeteredOperation(prefix = "role_app_",
            counters = @Counter(
                    name = "added_success_total",
                    conditionSpEL = "#result == true",
                    tags = @Tag(key = "role_name", valueSpEL = "#role.name()")
            )
    )
    public boolean addRole(String userEmail, Role role) {
        authCacheService.putExpiredData(userEmail);
        return userRepository.findByEmail(userEmail).map(user -> {
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            userRoleRepository.save(userRole);
            return true;
        }).orElseThrow(NotFoundUserException::new);
    }

    @Override
    @Transactional
    public List<Role> getUserRoles(String email){
        return userRoleRepository.findRolesByEmail(email);
    }

    @Override
    @Transactional
    @MeteredOperation(prefix = "role_app_",
            counters = @Counter(
                    name = "removed_success_total",
                    conditionSpEL = "#result == true",
                    tags = @Tag(key = "role_name", valueSpEL = "#role.name()")
            )
    )
    public boolean removeRole(String userEmail, Role role) {
        authCacheService.putExpiredData(userEmail);
        return userRepository.findByEmail(userEmail).map(user -> {
            userRoleRepository.deleteByUserAndRole(user, role);
            return true;
        }).orElseThrow(NotFoundUserException::new);
    }
}

package com.example.domain.security.service;

import com.example.database.model.company_subscription_module.user_roles.UserRole;
import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.database.repository.company_subscription_module.UserRoleRepository;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.security.model.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserContextLoader {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Value("${jwt.isLocal:false}")
    private boolean isLocal;

    private final Integer localDefaultUserId = 1;

    public UserContext loadUserContext() {
        if (isLocal) {
            if (localDefaultUserId == null || localDefaultUserId <= 0) {
                throw new IllegalStateException("Local development is enabled, but 'local.default.user.id' is not configured or invalid.");
            }

            User user = userRepository.findById(localDefaultUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Default local user with ID " + localDefaultUserId + " not found"));

            List<UserRole> userRoles = userRoleRepository.findByUserId(user.getId());
            Set<Role> roles = userRoles.stream()
                    .map(UserRole::getRole)
                    .collect(Collectors.toSet());

            return buildUserContext(user, roles);
        } else {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                throw new SecurityException("Authentication required to load user context.");
            }

            String username = authentication.getName();
            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> {
                        log.error("Authenticated user with username {} not found in database.", username);
                        return new ResourceNotFoundException("Authenticated user not found in database");
                    });

            List<UserRole> userRolesEntities = userRoleRepository.findByUserId(user.getId());
            Set<Role> roles = userRolesEntities.stream()
                    .map(UserRole::getRole)
                    .collect(Collectors.toSet());

            return buildUserContext(user, roles);
        }
    }

    private UserContext buildUserContext(User user, Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            //TODO Пока хз как обработать
            System.err.println("User " + user.getId() + " has no roles assigned. Context will have an empty role set.");
        }

        return UserContext.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .roles(roles)
                .companyId(user.getCompany() != null ? user.getCompany().getId() : null)
                .build();
    }
}

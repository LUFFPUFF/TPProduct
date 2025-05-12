package com.example.domain.security;

import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


@RequiredArgsConstructor
@Service
public class UserContextHolder {

    private final UserRepository userRepository;

    @Value("${jwt.isLocal}")
    private boolean isLocal;

    public UserContext getUserContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("Authentication required");
        }

        Integer userId = 1; //пользователь, который лежит у вас в базе

        if (isLocal) {
            return userRepository.findById(userId)
                    .map(user -> UserContext.builder()
                            .userId(user.getId())
                            .email(user.getEmail())
                            .role(Role.LOCAL)
                            .companyId(user.getCompany().getId())
                            .build())
                    .orElseThrow(() -> new ResourceNotFoundException("Default local user not found"));
        } else {
            return userRepository.findByEmail(authentication.getName())
                    .map(user -> UserContext.builder()
                            .userId(user.getId())
                            .email(user.getEmail())
                            .role(Role.valueOf(
                                    authentication.getAuthorities().stream()
                                            .findFirst()
                                            .map(GrantedAuthority::getAuthority)
                                            .orElse("USER")))
                            .companyId(user.getCompany() != null ? user.getCompany().getId() : null)
                            .build())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }
    }
}

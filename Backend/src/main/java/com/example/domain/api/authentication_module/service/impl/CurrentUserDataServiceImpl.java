package com.example.domain.api.authentication_module.service.impl;

import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.authentication_module.exception_handler_auth.NotFoundUserException;
import com.example.domain.api.authentication_module.service.interfaces.CurrentUserDataService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CurrentUserDataServiceImpl implements CurrentUserDataService {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public User getUser() {
        return userRepository.findByEmail(getUserEmail()).orElseThrow(NotFoundUserException::new);
    }

    @Override
    @Transactional
    public User getUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(NotFoundUserException::new);
    }

    @Override
    public boolean hasRole(Role role) {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(role);
    }

    @Override
    public String getUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Override
    public List<Role> getRoleList() {
        return SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().map(authority -> Role.valueOf(authority.getAuthority())).toList();
    }
}

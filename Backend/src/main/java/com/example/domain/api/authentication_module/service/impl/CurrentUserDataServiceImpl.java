package com.example.domain.api.authentication_module.service.impl;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.CompanyRepository;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.authentication_module.exception_handler_auth.NotFoundUserException;
import com.example.domain.api.authentication_module.service.interfaces.CurrentUserDataService;
import com.example.domain.api.company_module.exception_handler_company.NotFoundCompanyException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CurrentUserDataServiceImpl implements CurrentUserDataService {
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;


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
    @Transactional
    public Company getUserCompany() {
        return companyRepository.findById(getUser().getId()).orElseThrow(NotFoundCompanyException::new);
    }

    @Override
    public boolean hasRole(Role role) {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(role);
    }

    @Override
    public String getUserEmail() {
       try {
           return SecurityContextHolder.getContext().getAuthentication().getName();
       }catch (Exception e) {
           return null;
       }
    }

    @Override
    public List<Role> getRoleList() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return new ArrayList<>();
        }
        return auth.getAuthorities().stream()
                .map(authority -> {
                    try {
                        return Role.valueOf(authority.getAuthority());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }
}

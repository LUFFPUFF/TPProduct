package com.example.domain.api.authentication_module.service.impl;

import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.authentication_module.exception_handler_auth.NotFoundUserException;
import com.example.domain.api.authentication_module.service.interfaces.RoleService;
import com.example.domain.api.authentication_module.service.interfaces.UserDataService;
import com.example.domain.dto.UserCompanyRolesDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDataServiceImpl implements UserDataService {
    private final UserRepository userRepository;
    private final RoleService roleService;
    @Override
    @Transactional
    public UserCompanyRolesDto getUserData(String email) {
        UserCompanyRolesDto userData = userRepository.findUserData(email).orElseThrow(NotFoundUserException::new);
        userData.setUserRoles(roleService.getUserRoles(email));
        return userData;
    }
}

package com.example.domain.api.authentication_module.service.interfaces;

import com.example.domain.dto.UserCompanyRolesDto;

public interface UserDataService {
    UserCompanyRolesDto getUserData(String email);

}

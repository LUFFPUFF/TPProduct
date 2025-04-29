package com.example.domain.api.authentication_module.service.interfaces;

import com.example.domain.dto.RegistrationDto;
import com.example.domain.dto.TokenDto;

public interface RegistrationService {
    Boolean registerUser(RegistrationDto registrationDto);
    Boolean sendRegistrationCode(RegistrationDto registrationDto);
    TokenDto checkRegistrationCode(String registrationCode);
    void checkEmailIsAvailable(String email);
}

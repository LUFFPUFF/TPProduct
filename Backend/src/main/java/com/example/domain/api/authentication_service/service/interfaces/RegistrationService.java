package com.example.domain.api.authentication_service.service.interfaces;

import com.example.domain.dto.RegistrationDto;
import com.example.domain.dto.TokenDto;

public interface RegistrationService {
    TokenDto registerUser(RegistrationDto registrationDto);
    void checkEmailIsAvailable(String email);
}

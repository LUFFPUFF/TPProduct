package com.example.domain.api.authentication_module.cache;

import com.example.domain.dto.RegistrationDto;

import java.util.Optional;

public interface AuthCacheService {
    void putRefreshToken(String refreshToken, String email);
    void putRegistrationCode(String registrationCode, RegistrationDto registrationDto);
    Optional<RegistrationDto> getRegistrationCode(String registrationCode);
    void putChangePasswordCode(String changePasswordCode, String email);
    Optional<String> getChangePasswordCode(String email);
    String getEmail(String refreshToken);
    void removeRefreshToken(String refreshToken);
    void putExpiredData(String email);
    Optional<String> checkExpireToken(String refreshToken);
}

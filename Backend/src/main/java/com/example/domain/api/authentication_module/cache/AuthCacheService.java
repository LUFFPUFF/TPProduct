package com.example.domain.api.authentication_module.cache;

import com.example.domain.dto.RegistrationDto;

import java.util.Optional;

public interface AuthCacheService {
    void putRefreshToken(String refreshToken, String email);
    void putRegistrationCode(String registrationCode, RegistrationDto registrationDto);
    Optional<RegistrationDto> getRegistrationCode(String registrationCode);
    String getEmail(String refreshToken);
    void removeRefreshToken(String refreshToken);
    void putExpiredData(String email);
    Optional<String> checkExpireToken(String refreshToken);
}

package com.example.domain.api.authentication_service.cache;

import java.util.Optional;

public interface AuthCacheService {
    void putRefreshToken(String refreshToken, String email);
    String getEmail(String refreshToken);
    void removeRefreshToken(String refreshToken);
    void putExpiredData(String email);
    Optional<String> checkExpireToken(String refreshToken);
}

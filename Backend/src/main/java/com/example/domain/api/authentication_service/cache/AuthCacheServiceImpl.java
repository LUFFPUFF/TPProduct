package com.example.domain.api.authentication_service.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthCacheServiceImpl implements AuthCacheService {
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void putRefreshToken(String refreshToken, String email) {
        redisTemplate.opsForValue().set(refreshToken, email);
    }

    @Override
    public String getEmail(String refreshToken) {
        return redisTemplate.opsForValue().get(refreshToken);
    }

    @Override
    public void putExpiredData(String email) {
        redisTemplate.opsForValue().set("expired: " + email, email);
    }

    private void removeExpiredData(String email) {
        redisTemplate.delete("expired: " + email);
    }

    @Override
    public Optional<String> checkExpireToken(String refreshToken) {
        Optional<String> a = Optional.ofNullable( redisTemplate.opsForValue().get("expired: " +
                getEmail(refreshToken)));
        removeExpiredData(getEmail(refreshToken));
        return a;
    }

    @Override
    public void removeRefreshToken(String refreshToken) {
        redisTemplate.delete(refreshToken);
    }

}

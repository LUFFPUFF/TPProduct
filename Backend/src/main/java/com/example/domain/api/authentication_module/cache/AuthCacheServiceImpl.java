package com.example.domain.api.authentication_module.cache;

import com.example.domain.dto.RegistrationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthCacheServiceImpl implements AuthCacheService {
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisTemplate<String, RegistrationDto> registrationRedisTemplate;

    @Override
    public void putRegistrationCode(String registrationCode, RegistrationDto registrationDto) {
        String emailCode = redisTemplate.opsForValue().get("reg-email: " + registrationDto.getEmail());
        if(emailCode != null) {
            registrationRedisTemplate.delete("reg-code: "+emailCode);
        }
        redisTemplate.opsForValue().set("reg-email: " + registrationDto.getEmail(), registrationCode);
        redisTemplate.expire("reg-email: " + registrationCode, 8, TimeUnit.MINUTES);
        registrationRedisTemplate.opsForValue().set("reg-code: " + registrationCode, registrationDto);
        registrationRedisTemplate.expire("reg-code: " + registrationCode, 8, TimeUnit.MINUTES);
    }

    @Override
    public Optional<RegistrationDto> getRegistrationCode(String registrationCode) {
        return Optional.ofNullable(registrationRedisTemplate.opsForValue().get("reg-code: " + registrationCode));
    }

    @Override
    public void putChangePasswordCode(String changePasswordCode, String email) {
        String emailCache = redisTemplate.opsForValue().get("pass-code: " + changePasswordCode);
        if(emailCache != null) {
            registrationRedisTemplate.delete("pass-code: "+changePasswordCode);
        }
        redisTemplate.opsForValue().set("pass-code: " + changePasswordCode, email);
        redisTemplate.expire("pass-code: " + changePasswordCode, 8, TimeUnit.MINUTES);
    }

    @Override
    public Optional<String> getChangePasswordCode(String email) {
        return Optional.ofNullable(redisTemplate.opsForValue().get("pass-code: " + email));
    }

    @Override
    public void putRefreshToken(String refreshToken, String email) {
        redisTemplate.opsForValue().set(refreshToken, email);
        redisTemplate.expire(refreshToken, 4, TimeUnit.DAYS);
    }

    @Override
    public String getEmail(String refreshToken) {
        return redisTemplate.opsForValue().get(refreshToken);
    }

    @Override
    public void putExpiredData(String email) {
        redisTemplate.opsForValue().set("expired: " + email, email);
        redisTemplate.expire("expired: " + email, 3, TimeUnit.DAYS);
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

package com.example.domain.api.authentication_module.service.impl;

import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.authentication_module.cache.AuthCacheService;
import com.example.domain.api.authentication_module.exception_handler_auth.EmailExistsException;
import com.example.domain.api.authentication_module.exception_handler_auth.InvalidRegistrationCodeException;
import com.example.domain.api.authentication_module.security.jwtUtils.JWTUtilsService;
import com.example.domain.api.authentication_module.service.interfaces.RegistrationService;
import com.example.domain.dto.RegistrationDto;
import com.example.domain.dto.TokenDto;
import com.example.domain.dto.mapper.MapperDto;

import jakarta.security.auth.message.config.AuthConfig;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {
    private final UserRepository userRepository;
    private final MapperDto mapperDto;
    private final JWTUtilsService jWTUtilsService;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    private final AuthCacheService authCacheService;

    @Override
    @Transactional
    public Boolean registerUser(RegistrationDto registrationDto) {
        checkEmailIsAvailable(registrationDto.getEmail());
        registrationDto.setFullName(registrationDto.getEmail());
        registrationDto.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        registrationDto.setCreatedAt(LocalDateTime.now());
        registrationDto.setUpdatedAt(LocalDateTime.now());
        sendRegistrationCode(registrationDto);
        return true;
    }
    @Override
    @Transactional
    public Boolean sendRegistrationCode(RegistrationDto registrationDto) {
        //TODO: Отправка сообщения на email
        authCacheService.putRegistrationCode("000000", registrationDto);
        return true;
    }

    @Override
    public TokenDto checkRegistrationCode(String registrationCode) {
        return authCacheService.getRegistrationCode(registrationCode)
                .map( registrationDto ->{
                userRepository.save(mapperDto.toEntityUserFromRegistration(registrationDto));
               return jWTUtilsService.generateTokensByUser(userDetailsService.loadUserByUsername(registrationDto.getEmail()));
        })
        .orElseThrow(InvalidRegistrationCodeException::new);
    }

    private String generateRegistrationCode() {
        SecureRandom secureRandom = new SecureRandom();
        String code = String.valueOf(100000 + secureRandom.nextInt(900000));
        while (authCacheService.getRegistrationCode(code).isPresent()) {
            code = String.valueOf(100000 + secureRandom.nextInt(900000));
        }
        return code;
    }

    @Override
    @Transactional
    public void checkEmailIsAvailable(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new EmailExistsException();
        }
    }
}

package com.example.domain.api.authentication_module.service.impl;

import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.authentication_module.cache.AuthCacheService;
import com.example.domain.api.authentication_module.exception_handler_auth.EmailExistsException;
import com.example.domain.api.authentication_module.security.jwtUtils.JWTUtilsService;
import com.example.domain.api.authentication_module.service.interfaces.RegistrationService;
import com.example.domain.api.authentication_module.service.interfaces.RoleService;
import com.example.domain.api.statistics_module.aop.annotation.Counter;
import com.example.domain.api.statistics_module.aop.annotation.MeteredOperation;
import com.example.domain.api.statistics_module.aop.annotation.Timer;
import com.example.domain.api.statistics_module.metrics.service.IRegistrationMetricsService;
import com.example.domain.dto.RegistrationDto;
import com.example.domain.dto.TokenDto;
import com.example.domain.dto.mapper.MapperDto;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@MeteredOperation(prefix = "registration_app_",
        timers = {
                @Timer(name = "operation_duration_seconds", description = "Duration of registration operations")
        }
)
public class RegistrationServiceImpl implements RegistrationService {
    private final UserRepository userRepository;
    private final MapperDto mapperDto;
    private final JWTUtilsService jWTUtilsService;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    private final AuthCacheService authCacheService;
    private final RoleService roleService;
    private final IRegistrationMetricsService registrationMetricsService;

    @Override
    @Transactional
    public Boolean registerUser(RegistrationDto registrationDto) {
        registrationMetricsService.incrementUserRegistrationAttempt();

        try {
            if (checkEmailIsAvailable(registrationDto.getEmail())) {
                registrationDto.setFullName(registrationDto.getEmail());
                registrationDto.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
                registrationDto.setCreatedAt(LocalDateTime.now());
                registrationDto.setUpdatedAt(LocalDateTime.now());
                sendRegistrationCode(registrationDto);
            }
            return true;
        } catch (EmailExistsException e) {
            throw e;
        } catch (Exception e) {
            registrationMetricsService.incrementRegistrationOperationError("registerUser", e.getClass().getSimpleName());
            throw new RuntimeException("User registration failed due to an unexpected error", e);
        }
    }

    @Override
    @Transactional
    @MeteredOperation(
            prefix = "registration_app_",
            counters = {
                    @Counter(name = "code_sent_success_total", conditionSpEL = "#result == true"),
                    @Counter(name = "code_sent_failure_total", conditionSpEL = "#result == false")
            }
    )
    public Boolean sendRegistrationCode(RegistrationDto registrationDto) {
        //TODO: Отправка сообщения на email
        authCacheService.putRegistrationCode("000000", registrationDto);
        return true;
    }

    @Override
    @Transactional
    public TokenDto checkRegistrationCode(String registrationCode) {
        try {
            return authCacheService.getRegistrationCode(registrationCode)
                    .map(registrationDto -> {
                        User newUser = mapperDto.toEntityUserFromRegistration(registrationDto);
                        userRepository.save(newUser);
                        roleService.addRole(registrationDto.getEmail(), Role.USER);
                        TokenDto generatedTokens = jWTUtilsService.generateTokensByUser(userDetailsService.loadUserByUsername(registrationDto.getEmail()));
                        registrationMetricsService.incrementRegistrationCodeCheckSuccess();
                        return generatedTokens;
                    })
                    .orElseThrow(() -> {
                        registrationMetricsService.incrementRegistrationCodeCheckFailureInvalidCode();
                        return new RuntimeException("User registration failed due to an unexpected error");
                    });
        } catch (Exception e) {
            registrationMetricsService.incrementRegistrationOperationError("checkRegistrationCode", e.getClass().getSimpleName());
            throw new RuntimeException("Code check failed due to an unexpected error", e);
        }
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
    public boolean checkEmailIsAvailable(String email) {
        try {
            if (userRepository.findByEmail(email).isPresent()) {
                registrationMetricsService.incrementRegistrationEmailExists();
                throw new EmailExistsException();
            }
            return true;
        } catch (EmailExistsException e) {
            throw e;
        } catch (Exception e) {
            registrationMetricsService.incrementRegistrationOperationError("checkEmailIsAvailable", e.getClass().getSimpleName());
            throw new RuntimeException("Email availability check failed", e);
        }
    }
}

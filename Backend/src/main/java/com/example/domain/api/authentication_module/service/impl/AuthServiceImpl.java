package com.example.domain.api.authentication_module.service.impl;

import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.authentication_module.cache.AuthCacheService;
import com.example.domain.api.authentication_module.exception_handler_auth.NotFoundUserException;
import com.example.domain.api.authentication_module.exception_handler_auth.WrongPasswordException;
import com.example.domain.api.authentication_module.security.jwtUtils.JWTUtilsService;
import com.example.domain.api.authentication_module.service.interfaces.AuthService;
import com.example.domain.api.statistics_module.aop.annotation.Counter;
import com.example.domain.api.statistics_module.aop.annotation.MeteredOperation;
import com.example.domain.api.statistics_module.aop.annotation.Timer;
import com.example.domain.api.statistics_module.metrics.service.IAuthMetricsService;
import com.example.domain.dto.TokenDto;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@MeteredOperation(prefix = "auth_app_",
        timers = {
                @Timer(name = "operation_duration_seconds", description = "Duration of authentication operations")
        },
        counters = {
                @Counter(name = "operation_errors_total", description = "Total errors during authentication operations",
                        conditionSpEL = "#throwable != null"
                )
        }
)
public class AuthServiceImpl implements AuthService {
    private final JWTUtilsService jwtUtilsService;
    private final UserRepository userRepository;
    private final AuthCacheService authCacheService;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final IAuthMetricsService authMetricsService;

    @Override
    public TokenDto login(String email, String password) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        authMetricsService.incrementLoginFailureUserNotFound();
                        return new NotFoundUserException();
                    });

            if (!passwordEncoder.matches(password, user.getPassword())) {
                authMetricsService.incrementLoginFailureWrongPassword();
                throw new WrongPasswordException();
            }

            TokenDto tokens = jwtUtilsService.generateTokensByUser(
                    userDetailsService.loadUserByUsername(user.getEmail())
            );
            authMetricsService.incrementLoginSuccess();
            return tokens;
        } catch (NotFoundUserException | WrongPasswordException e) {
            throw e;
        } catch (Exception e) {
            authMetricsService.incrementAuthOperationError("login", e.getClass().getSimpleName());
            throw new RuntimeException("Login process failed due to an unexpected error", e);
        }
    }


    @Override
    public boolean logout(String refreshToken) {
        try {
            if (refreshToken == null || refreshToken.isBlank()) {
                return false;
            }
            authCacheService.removeRefreshToken(refreshToken);
            authMetricsService.incrementLogoutSuccess();
            return true;
        } catch (Exception e) {
            authMetricsService.incrementAuthOperationError("logout", e.getClass().getSimpleName());
            throw new RuntimeException("Logout process failed", e);
        }
    }
}

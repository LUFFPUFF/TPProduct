package com.example.domain.api.authentication_service.service.impl;

import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.authentication_service.cache.AuthCacheService;
import com.example.domain.api.authentication_service.exception_handler_auth.NoFoundUserException;
import com.example.domain.api.authentication_service.exception_handler_auth.WrongPasswordException;
import com.example.domain.api.authentication_service.security.jwtUtils.JWTUtilsService;
import com.example.domain.api.authentication_service.service.interfaces.AuthService;
import com.example.domain.dto.TokenDto;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final JWTUtilsService jwtUtilsService;
    private final UserRepository userRepository;
    private final AuthCacheService authCacheService;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public TokenDto login(String email, String password) {
        return userRepository.findByEmail(email)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .map(user -> jwtUtilsService.generateTokensByUser(userDetailsService.loadUserByUsername(user.getEmail())))
                .orElseThrow(NoFoundUserException::new);
    }

    @Override
    public boolean logout(String refreshToken) {
        authCacheService.removeRefreshToken(refreshToken);
        return true;
    }
}

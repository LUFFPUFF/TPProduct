package com.example.domain.api.authentication_module.service.impl;

import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.authentication_module.cache.AuthCacheService;
import com.example.domain.api.authentication_module.exception_handler_auth.NotFoundUserException;
import com.example.domain.api.authentication_module.exception_handler_auth.WrongPasswordException;
import com.example.domain.api.authentication_module.security.jwtUtils.JWTUtilsService;
import com.example.domain.api.authentication_module.service.interfaces.AuthService;
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
        User user = userRepository.findByEmail(email)
                .orElseThrow(NotFoundUserException::new);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new WrongPasswordException();
        }
        return jwtUtilsService.generateTokensByUser(
                userDetailsService.loadUserByUsername(user.getEmail())
        );
    }
    @Override
    public boolean logout(String refreshToken) {
        authCacheService.removeRefreshToken(refreshToken);
        return true;
    }
}

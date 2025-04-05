package com.example.domain.api.authentication_service.service.interfaces;

import com.example.domain.dto.TokenDto;

public interface AuthService {

    TokenDto login(String email, String password);
    boolean logout(String refreshToken);
}

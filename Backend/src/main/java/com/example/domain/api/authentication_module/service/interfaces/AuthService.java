package com.example.domain.api.authentication_module.service.interfaces;

import com.example.domain.dto.TokenDto;

public interface AuthService {

    TokenDto login(String email, String password);
    boolean logout(String refreshToken);
}

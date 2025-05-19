package com.example.domain.api.authentication_module.service.interfaces;

import com.example.domain.api.authentication_module.dto.AuthDataDto;
import com.example.domain.dto.TokenDto;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    TokenDto login(String email, String password);
    boolean logout(String refreshToken);
    AuthDataDto getData(HttpServletRequest request);
}

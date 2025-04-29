package com.example.domain.api.authentication_module.security.jwtUtils;

import com.example.domain.dto.TokenDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthCookieService {
    void setTokenCookies(HttpServletResponse response, TokenDto tokenDto);
    TokenDto getTokensCookie(HttpServletRequest request);
    void ExpireTokenCookie(HttpServletResponse response);
}

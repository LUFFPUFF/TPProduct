package com.example.domain.api.authentication_service.security.jwtUtils;

import com.example.domain.dto.TokenDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthCookieService {
    void setTokenCookies(HttpServletResponse response, TokenDto tokenDto);
    TokenDto getTokensCookie(HttpServletRequest request);
    void ExpireTokenCookie(HttpServletResponse response);
}

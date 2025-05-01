package com.example.domain.api.authentication_module.security.jwtUtils.impl;

import com.example.domain.api.authentication_module.security.jwtUtils.AuthCookieService;
import com.example.domain.dto.TokenDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthCookieServiceImpl implements AuthCookieService {

    public void setTokenCookies(HttpServletResponse resp, TokenDto tokenDto) {
        Cookie access_cookie = new Cookie("access_token", tokenDto.getAccess_token());
        access_cookie.setHttpOnly(true);
        access_cookie.setSecure(true);
        access_cookie.setPath("/");
        access_cookie.setMaxAge(60 * 15);

        Cookie refresh_cookie = new Cookie("refresh_token", tokenDto.getRefresh_token());
        refresh_cookie.setHttpOnly(true);
        refresh_cookie.setSecure(true);
        refresh_cookie.setPath("/");
        refresh_cookie.setMaxAge(60 * 60 * 24 * 3);

        resp.addCookie(access_cookie);
        resp.addCookie(refresh_cookie);
    }

    public TokenDto getTokensCookie(HttpServletRequest request) {
        String access_token = "";
        String refresh_token = "";
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) {
                    access_token = cookie.getValue();
                } else if ("refresh_token".equals(cookie.getName())) {
                    refresh_token = cookie.getValue();
                }
            }
        }
        return TokenDto.builder()
                .access_token(access_token)
                .refresh_token(refresh_token).build();
    }

    public void ExpireTokenCookie(HttpServletResponse resp) {
        Cookie access_cookie = new Cookie("access_token", "");
        access_cookie.setHttpOnly(true);
        access_cookie.setSecure(true);
        access_cookie.setPath("/");
        access_cookie.setMaxAge(0);

        Cookie refresh_cookie = new Cookie("refresh_token", "");
        refresh_cookie.setHttpOnly(true);
        refresh_cookie.setSecure(true);
        refresh_cookie.setPath("/");
        refresh_cookie.setMaxAge(0);

        resp.addCookie(access_cookie);
        resp.addCookie(refresh_cookie);

    }

}

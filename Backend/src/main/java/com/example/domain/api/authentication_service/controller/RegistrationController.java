package com.example.domain.api.authentication_service.controller;

import com.example.domain.api.authentication_service.security.jwtUtils.AuthCookieService;
import com.example.domain.api.authentication_service.service.interfaces.RegistrationService;
import com.example.domain.dto.RegistrationDto;
import com.example.domain.dto.TokenDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/registration")
@RequiredArgsConstructor
public class RegistrationController {
    private final RegistrationService registrationService;
 private final AuthCookieService authCookieService;

    @PostMapping("/register")
    public ResponseEntity<String> registration(@RequestBody RegistrationDto registrationDto, HttpServletResponse resp) {
        TokenDto tokenDto = registrationService.registerUser(registrationDto);
        authCookieService.setTokenCookies(resp, tokenDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(tokenDto.getAccess_token());
    }

    @GetMapping("/check-email")
    public boolean checkEmailIsAvailable() {
        return false;
    }

}

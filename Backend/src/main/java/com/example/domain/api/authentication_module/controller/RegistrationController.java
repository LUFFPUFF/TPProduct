package com.example.domain.api.authentication_module.controller;

import com.example.domain.api.authentication_module.security.jwtUtils.AuthCookieService;
import com.example.domain.api.authentication_module.security.jwtUtils.JWTUtilsService;
import com.example.domain.api.authentication_module.service.interfaces.RegistrationService;
import com.example.domain.dto.CheckCodeDto;
import com.example.domain.dto.EmailDto;
import com.example.domain.dto.RegistrationDto;
import com.example.domain.dto.TokenDto;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/registration")
@RequiredArgsConstructor
public class RegistrationController {
    private final RegistrationService registrationService;
    private final AuthCookieService authCookieService;
    private final JWTUtilsService jwtUtilsService;

    @PostMapping("/register")
    public ResponseEntity<Boolean> registration(@RequestBody @Validated RegistrationDto registrationDto, HttpServletResponse resp) {
        return ResponseEntity.ok(registrationService.registerUser(registrationDto));
    }

    @PostMapping("/check-code")
    public ResponseEntity<EmailDto> checkCode(@RequestBody @Validated CheckCodeDto code, HttpServletResponse resp) {
        TokenDto tokenDto = registrationService.checkRegistrationCode(code.getCode());
        authCookieService.setTokenCookies(resp, tokenDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                EmailDto.builder()
                        .email(jwtUtilsService.getEmail(tokenDto.getAccess_token() ))
                .build());
    }

}

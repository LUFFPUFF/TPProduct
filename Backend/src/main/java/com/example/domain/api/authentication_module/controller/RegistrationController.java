package com.example.domain.api.authentication_module.controller;

import com.example.domain.api.authentication_module.security.jwtUtils.AuthCookieService;
import com.example.domain.api.authentication_module.service.interfaces.RegistrationService;
import com.example.domain.dto.RegistrationDto;
import com.example.domain.dto.TokenDto;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/registration")
@RequiredArgsConstructor
public class RegistrationController {
    private final RegistrationService registrationService;
 private final AuthCookieService authCookieService;

    @PostMapping("/register")
    public ResponseEntity<Boolean> registration(@RequestBody @Validated RegistrationDto registrationDto, HttpServletResponse resp) {
        return ResponseEntity.ok(registrationService.registerUser(registrationDto));
    }
    @PostMapping("/check-code")
    public ResponseEntity<String> checkCode(@RequestParam String code, HttpServletResponse resp) {
        TokenDto tokenDto = registrationService.checkRegistrationCode(code);
        authCookieService.setTokenCookies(resp, tokenDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(tokenDto.getAccess_token());
    }

    @GetMapping("/check-email")
    public boolean checkEmailIsAvailable() {
        return false;
    }

}

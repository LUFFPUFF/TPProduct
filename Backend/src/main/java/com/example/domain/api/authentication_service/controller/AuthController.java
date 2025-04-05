package com.example.domain.api.authentication_service.controller;

import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.authentication_service.security.jwtUtils.AuthCookieService;
import com.example.domain.api.authentication_service.service.interfaces.AuthService;
import com.example.domain.dto.TokenDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final UserRepository userRepository;
    private final AuthCookieService authCookieService;

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String email,@RequestParam String password, HttpServletResponse resp) {
        TokenDto tokenDto = authService.login(email,password);
        authCookieService.setTokenCookies(resp,tokenDto);
        return ResponseEntity.ok().body("Залогинился");
    }

    @PostMapping("/logout")
    public ResponseEntity<Boolean> logout(HttpServletRequest req, HttpServletResponse resp) {
        authService.logout(authCookieService.getTokensCookie(req).getRefresh_token());
        authCookieService.ExpireTokenCookie(resp);
        return ResponseEntity.ok().body(true);
    }
    @GetMapping("/data/{id}")
    public ResponseEntity<User> getData(@PathVariable("id") String id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userRepository.findById(Integer.valueOf(id)).get());
    }
}

package com.example.domain.api.authentication_module.controller;

import com.example.domain.api.authentication_module.dto.AnswerSettingsDto;
import com.example.domain.api.authentication_module.dto.ChangePasswordCodeDto;
import com.example.domain.api.authentication_module.dto.PasswordDto;
import com.example.domain.api.authentication_module.dto.UserDataDto;
import com.example.domain.api.authentication_module.service.interfaces.UserSettingsService;
import com.example.domain.dto.CheckCodeDto;
import com.example.domain.dto.EmailDto;
import com.example.domain.dto.TokenDto;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class UserSettingsController {
    private final UserSettingsService userSettingsService;
    @GetMapping("/get")
    public ResponseEntity<UserDataDto> getUserData(){
        return null;
    }

    @PostMapping("/set")
    public ResponseEntity<UserDataDto> setUserData(@RequestBody UserDataDto userDataDto){
        return ResponseEntity.status(HttpStatus.CREATED).body(userSettingsService.setUserData(userDataDto));
    }
    @PostMapping("/change-password/password")
    public ResponseEntity<AnswerSettingsDto> changePassword(@RequestBody @Validated PasswordDto passwordDto){
        return ResponseEntity.ok(userSettingsService.changePassword(passwordDto));
    }
    @PostMapping("/change-password/emailCode")
    public ResponseEntity<AnswerSettingsDto> changePassword(EmailDto emailDto){
        return ResponseEntity.ok(userSettingsService.changePassword(emailDto));
    }
    @PostMapping("/check-code")
    public ResponseEntity<AnswerSettingsDto> checkCode(@RequestBody @Validated ChangePasswordCodeDto code, HttpServletResponse resp) {
        return ResponseEntity.ok(userSettingsService.checkCode(code));
    }

    //TODO: Опционально
//    @PostMapping("/email-expire/password")
//    public ResponseEntity<PasswordDto> registerNewEmail(@RequestBody PasswordDto passwordDto){
//        return null;
//    }

}

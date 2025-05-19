package com.example.domain.api.authentication_module.service.interfaces;

import com.example.domain.api.authentication_module.dto.AnswerSettingsDto;
import com.example.domain.api.authentication_module.dto.ChangePasswordCodeDto;
import com.example.domain.api.authentication_module.dto.PasswordDto;
import com.example.domain.api.authentication_module.dto.UserDataDto;

import com.example.domain.dto.EmailDto;


public interface UserSettingsService {
    UserDataDto getUserData();

    UserDataDto setUserData(UserDataDto userDataDto);

    AnswerSettingsDto changePassword(PasswordDto passwordDto);

    AnswerSettingsDto changePassword(EmailDto emailDto);

    AnswerSettingsDto checkCode(ChangePasswordCodeDto code);


}

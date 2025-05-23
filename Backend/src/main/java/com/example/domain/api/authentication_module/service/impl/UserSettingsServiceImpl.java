package com.example.domain.api.authentication_module.service.impl;

import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.authentication_module.cache.AuthCacheService;
import com.example.domain.api.authentication_module.dto.AnswerSettingsDto;
import com.example.domain.api.authentication_module.dto.ChangePasswordCodeDto;
import com.example.domain.api.authentication_module.dto.PasswordDto;
import com.example.domain.api.authentication_module.dto.UserDataDto;
import com.example.domain.api.authentication_module.exception_handler_auth.InvalidCodeException;
import com.example.domain.api.authentication_module.exception_handler_auth.NotFoundCodeException;
import com.example.domain.api.authentication_module.exception_handler_auth.WrongPasswordException;
import com.example.domain.api.authentication_module.mapper.UserToUserDataMapper;
import com.example.domain.api.authentication_module.service.interfaces.CurrentUserDataService;
import com.example.domain.api.authentication_module.service.interfaces.UserSettingsService;
import com.example.domain.dto.EmailDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class UserSettingsServiceImpl implements UserSettingsService {
    private final UserRepository userRepository;
    private final CurrentUserDataService currentUserDataService;
    private final UserToUserDataMapper userToUserDataMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthCacheService authCacheService;

    @Override
    public UserDataDto getUserData() {
        return userToUserDataMapper.map(currentUserDataService.getUser());
    }

    @Override
    @Transactional
    public UserDataDto setUserData(UserDataDto userDataDto) {

        User user = currentUserDataService.getUser();
        user.setDateOfBirth(userDataDto.getBirthday());
        user.setGender(userDataDto.getGender());
        user.setFullName(userDataDto.getFullName());
        userRepository.save(user);
        return userDataDto;
    }

    @Override
    @Transactional
    public AnswerSettingsDto changePassword(PasswordDto passwordDto) {
        User user = currentUserDataService.getUser();
        if (!passwordEncoder.matches(passwordDto.getPassword(), user.getPassword())) {
            throw new WrongPasswordException();
        }
        user.setPassword(passwordEncoder.encode(passwordDto.getPassword()));
        userRepository.save(user);
        return AnswerSettingsDto.builder().answer("Успешно").build();
    }

    @Override
    public AnswerSettingsDto changePassword(EmailDto emailDto) {
        //TODO: отправка сообщения на email
        authCacheService.putChangePasswordCode("000000", emailDto.getEmail());
        return null;
    }

    @Override
    @Transactional
    public AnswerSettingsDto checkCode(ChangePasswordCodeDto code) {
        return  authCacheService.getChangePasswordCode(code.getEmail())
                .map(cacheCode -> {
                    if(cacheCode.equals(code.getCode())){
                        User user = currentUserDataService.getUser(code.getEmail());
                        user.setPassword(code.getCode());
                        return AnswerSettingsDto.builder().answer("Успешно").build();
                    }else{
                        throw new InvalidCodeException();
                    }

                }).orElseThrow(NotFoundCodeException::new);

    }

    private String generateChangePasswordCode() {
        SecureRandom secureRandom = new SecureRandom();
        String code = String.valueOf(100000 + secureRandom.nextInt(900000));
        while (authCacheService.getRegistrationCode(code).isPresent()) {
            code = String.valueOf(100000 + secureRandom.nextInt(900000));
        }
        return code;
    }
}

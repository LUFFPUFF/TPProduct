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
import com.example.domain.api.authentication_module.exception_handler_auth.NotFoundUserException;
import com.example.domain.api.authentication_module.exception_handler_auth.WrongPasswordException;
import com.example.domain.api.authentication_module.mapper.UserToUserDataMapper;
import com.example.domain.api.authentication_module.service.interfaces.CurrentUserDataService;
import com.example.domain.api.authentication_module.service.interfaces.UserSettingsService;
import com.example.domain.api.chat_service_api.integration.manager.mail.manager.EmailDialogManager;
import com.example.domain.dto.EmailDto;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Properties;

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
        user.setFullName(userDataDto.getName());
        userRepository.save(user);
        return userDataDto;
    }

    @Override
    @Transactional
    public AnswerSettingsDto changePassword(PasswordDto passwordDto) {
        User user = currentUserDataService.getUser();
        if (!passwordEncoder.matches(passwordDto.getOld(), user.getPassword())) {
            throw new WrongPasswordException();
        }
        user.setPassword(passwordEncoder.encode(passwordDto.getPassword()));
        userRepository.save(user);
        return AnswerSettingsDto.builder().answer("Успешно").build();
    }

    @Override
    public AnswerSettingsDto changePassword(EmailDto emailDto) {
        String code = generateChangePasswordCode();
        sendEmailMessage("dialogxcompany@gmail.com","smtp.gmail.com","ajss annz efsh kacm"
                ,emailDto.getEmail(),"Восстановление пароля","Код: "+ code);

        authCacheService.putChangePasswordCode(code, emailDto.getEmail());
        return null;
    }

    @Override
    @Transactional
    public AnswerSettingsDto checkCode(ChangePasswordCodeDto code) {
        return  authCacheService.getChangePasswordCode(code.getEmail())
                .map(cacheCode -> {
                    if(cacheCode.equals(code.getCode())){
                        User user = userRepository.findByEmail(code.getEmail()).orElseThrow(NotFoundUserException::new);
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

    public void sendEmailMessage(String fromEmailAddress,
                                 String smtpHost,
                                 String appPassword,
                                 String toEmailAddress,
                                 String subject,
                                 String content) {





        int smtpPort = 587;

        JavaMailSenderImpl dynamicMailSender = new JavaMailSenderImpl();
        dynamicMailSender.setHost(smtpHost);
        dynamicMailSender.setPort(smtpPort);
        dynamicMailSender.setUsername(fromEmailAddress);
        dynamicMailSender.setPassword(appPassword);

        Properties props = dynamicMailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");

        //TODO пока что так, в проде такое нельзя делать
        props.put("mail.smtp.starttls.enable", "true");

        props.put("mail.smtp.ssl.trust", smtpHost);

        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        MimeMessage mimeMessage = dynamicMailSender.createMimeMessage();
        try {
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            mimeMessageHelper.setFrom(fromEmailAddress);
            mimeMessageHelper.setTo(toEmailAddress);
            mimeMessageHelper.setSubject(subject != null && !subject.trim().isEmpty() ? subject : "Без темы");
            mimeMessageHelper.setText(content, false);

            dynamicMailSender.send(mimeMessage);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }
}

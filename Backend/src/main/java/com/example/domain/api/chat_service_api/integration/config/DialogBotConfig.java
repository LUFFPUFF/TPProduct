package com.example.domain.api.chat_service_api.integration.config;

import com.example.database.model.company_subscription_module.company.CompanyMailConfiguration;
import com.example.database.model.company_subscription_module.company.CompanyTelegramConfiguration;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.CompanyMailConfigurationRepository;
import com.example.database.repository.company_subscription_module.CompanyTelegramConfigurationRepository;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.integration.mail.dialog_bot.EmailDialogBot;
import com.example.domain.api.chat_service_api.integration.telegram.TelegramDialogBot;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Configuration
@RequiredArgsConstructor
public class DialogBotConfig {

    private final CompanyTelegramConfigurationRepository companyTelegramConfigurationRepository;
    private final CompanyMailConfigurationRepository companyMailConfigurationRepository;
    private final UserRepository userRepository;

    @Bean
    public TelegramDialogBot createDialogBot(BlockingQueue<Object> incomingMessageQueue,
                                             CompanyTelegramConfigurationRepository companyTelegramConfigurationRepository) {

//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//
//        Optional<User> currentUserOpt = getCurrentAppUser(authentication.getName());
//
//        User currentUser = currentUserOpt
//                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Optional<CompanyTelegramConfiguration> companyTelegramConfiguration = companyTelegramConfigurationRepository
                .findByCompanyId(2);

        String botToken = null;
        String botUsername = null;

        if (companyTelegramConfiguration.isPresent()) {
            CompanyTelegramConfiguration companyTelegramConfigurationCopy = companyTelegramConfiguration.get();

            botToken = companyTelegramConfigurationCopy.getBotToken();
            botUsername = companyTelegramConfigurationCopy.getBotUsername();
        }

        return new TelegramDialogBot(botToken, botUsername, incomingMessageQueue, companyTelegramConfigurationRepository);
    }

    private Optional<User> getCurrentAppUser(String email) {
        return userRepository.findByEmail(email);
    }


}

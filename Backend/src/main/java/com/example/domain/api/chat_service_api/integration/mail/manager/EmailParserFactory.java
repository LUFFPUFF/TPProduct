package com.example.domain.api.chat_service_api.integration.mail.manager;

import com.example.database.model.company_subscription_module.company.CompanyMailConfiguration;
import com.example.domain.api.chat_service_api.integration.mail.parser.AbstractEmailParser;
import com.example.domain.api.chat_service_api.integration.mail.parser.GmailParser;
import com.example.domain.api.chat_service_api.integration.mail.parser.YandexParser;
import com.example.domain.api.chat_service_api.integration.mail.properties.EmailProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;

@Component
public class EmailParserFactory {

    private final EmailProperties emailProperties;
    private final BlockingQueue<Object> incomingMessageQueue;

    public EmailParserFactory(EmailProperties emailProperties,
                              BlockingQueue<Object> incomingMessageQueue) {
        this.emailProperties = emailProperties;
        this.incomingMessageQueue = incomingMessageQueue;
    }

    public AbstractEmailParser createParser(CompanyMailConfiguration config) {
        if (config == null || config.getEmailAddress() == null || config.getEmailAddress().trim().isEmpty() ||
                config.getAppPassword() == null || config.getAppPassword().trim().isEmpty() ||
                config.getImapServer() == null || config.getImapServer().trim().isEmpty() ||
                config.getCompany() == null || config.getCompany().getId() == null) {
            throw new IllegalArgumentException("Неполная или некорректная конфигурация почты для создания парсера.");
        }

        if (config.getImapServer().contains(emailProperties.getImapHostGmail())) {
            return new GmailParser(config, incomingMessageQueue, emailProperties);
        } else if (config.getImapServer().contains(emailProperties.getImapHostYandex())) {
            return new YandexParser(config, incomingMessageQueue, emailProperties);
        }
        throw new IllegalArgumentException("Неизвестный почтовый сервер '" + config.getImapServer() + "' для почты " + config.getEmailAddress());
    }
}

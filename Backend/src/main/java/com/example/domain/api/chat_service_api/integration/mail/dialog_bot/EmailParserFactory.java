package com.example.domain.api.chat_service_api.integration.mail.dialog_bot;

import com.example.database.model.company_subscription_module.company.CompanyMailConfiguration;
import com.example.domain.api.chat_service_api.integration.mail.parser.AbstractEmailParser;
import com.example.domain.api.chat_service_api.integration.mail.parser.GmailParser;
import com.example.domain.api.chat_service_api.integration.mail.parser.YandexParser;
import com.example.domain.api.chat_service_api.integration.mail.properties.EmailProperties;
import com.example.domain.api.chat_service_api.integration.mail.response.EmailResponse;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;

@Component
public class EmailParserFactory {

    private final EmailProperties emailProperties;

    public EmailParserFactory(EmailProperties emailProperties) {
        this.emailProperties = emailProperties;
    }

    public AbstractEmailParser createParser(CompanyMailConfiguration config, BlockingQueue<EmailResponse> messageQueue) {
        if (config.getImapServer().contains(emailProperties.getImapHostGmail())) {
            return new GmailParser(config, messageQueue, emailProperties);
        } else if (config.getImapServer().contains(emailProperties.getImapHostYandex())) {
            return new YandexParser(config, messageQueue, emailProperties);
        }
        throw new IllegalArgumentException("Неизвестный почтовый сервер: " + config.getImapServer());
    }
}

package com.example.domain.api.chat_service_api.integration.mail.parser;

import com.example.database.model.company_subscription_module.company.CompanyMailConfiguration;
import com.example.domain.api.chat_service_api.integration.mail.properties.EmailProperties;
import com.example.domain.api.chat_service_api.integration.mail.response.EmailResponse;

import java.util.Properties;
import java.util.concurrent.BlockingQueue;

public class YandexParser extends AbstractEmailParser {

    private final EmailProperties emailProperties;

    public YandexParser(CompanyMailConfiguration emailConfig,
                        BlockingQueue<EmailResponse> messageQueue,
                        EmailProperties emailProperties) {
        super(emailConfig, emailProperties, messageQueue);
        this.emailProperties = emailProperties;
    }

    @Override
    protected Properties getMailProperties() {
        Properties props = new Properties();
        props.put("mail.imap.host", emailProperties.getImapHostYandex());
        props.put("mail.imap.port", emailProperties.getImapPort());
        props.put("mail.imap.ssl.enable", emailProperties.getImapSslEnable());
        return props;
    }
}

package com.example.domain.api.chat_service_api.integration.mail.parser;

import com.example.database.model.company_subscription_module.company.CompanyMailConfiguration;
import com.example.domain.api.chat_service_api.integration.mail.properties.EmailProperties;

import java.util.Properties;
import java.util.concurrent.BlockingQueue;

public class GmailParser extends AbstractEmailParser {

    public GmailParser(CompanyMailConfiguration emailConfig,
                       BlockingQueue<Object> incomingMessageQueue,
                       EmailProperties emailProperties) {
        super(emailConfig, incomingMessageQueue, emailProperties);
    }

    @Override
    protected Properties getMailProperties() {
        Properties props = new Properties();
        props.put("mail.imap.host", emailProperties.getImapHostGmail());
        props.put("mail.imap.port", emailProperties.getImapPort());
        props.put("mail.imap.ssl.enable", emailProperties.getImapSslEnable());
        return props;
    }
}

package com.example.domain.api.chat_service_api.integration.manager.mail.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;


@Configuration
@Data
public class EmailProperties {

    @Value("${email.imap_host.yandex}")
    private String imapHostYandex;

    @Value("${email.imap_host.gmail}")
    private String imapHostGmail;

    @Value("${email.imap_port}")
    private String imapPort;

    @Value("${email.imap_ssl_enable}")
    private String imapSslEnable;

    @Value("${email.imap_store}")
    private String imapStore;

    @Value("${email.imap_folder}")
    private String imapFolder;

    @Value("${email.polling_interval_ms:10000}")
    private Long pollingIntervalMs;
}

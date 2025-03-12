package com.example.domain.api.chat_service_api.integration.mail.email;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter @Setter
public class EmailProperties {

    @Value("${email.test_user.host}")
    private String host;

    @Value("${email.test_user.port}")
    private int port;

    @Value("${email.test_user.username}")
    private String username;

    @Value("${email.test_user.password}")
    private String password;

    @Value("${email.test_user.protocol}")
    private String protocol;

    @Value("${email.test_user.folder}")
    private String folderName;
}

package com.example.domain.api.chat_service_api.service.impl;

import com.example.domain.api.chat_service_api.service.WebSocketMessagingService;
import com.example.domain.api.chat_service_api.service.security.IChatSecurityService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.mockito.Mockito;

@EnableJpaRepositories(basePackages = {
        "com.example.database.repository.chats_messages_module",
        "com.example.database.repository.company_subscription_module",
        "com.example.database.repository.crm_module"
})
@EntityScan(basePackages = {
        "com.example.database.model.chats_messages_module",
        "com.example.database.model.company_subscription_module",
        "com.example.database.model.crm_module"
})
@TestConfiguration
public class TestJpaConfig {

    @Bean
    public WebSocketMessagingService messagingService() {
        return Mockito.mock(WebSocketMessagingService.class);
    }

    @Bean
    public IChatSecurityService chatSecurityService() {
        IChatSecurityService mock = Mockito.mock(IChatSecurityService.class);
        Mockito.when(mock.canProcessAndSaveMessage(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.when(mock.canAccessChat(Mockito.any())).thenReturn(true);
        Mockito.when(mock.canUpdateMessageStatus(Mockito.any())).thenReturn(true);
        Mockito.when(mock.canMarkMessagesAsRead(Mockito.any(), Mockito.any())).thenReturn(true);
        return mock;
    }
}
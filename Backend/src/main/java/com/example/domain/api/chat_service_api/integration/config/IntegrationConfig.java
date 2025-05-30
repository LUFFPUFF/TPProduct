package com.example.domain.api.chat_service_api.integration.config;

import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.domain.api.chat_service_api.integration.sender.ChannelSender;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.client.RestTemplate;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Configuration
public class IntegrationConfig {

    @Bean
    public BlockingQueue<Object> incomingMessageQueue() {
        return new LinkedBlockingQueue<>();
    }

    @Bean
    public BlockingQueue<Object> outgoingMessageQueue() {
        return new LinkedBlockingQueue<>();
    }

    @Bean
    public JavaMailSender getJavaMailSender() {
        return new JavaMailSenderImpl();
    }

    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public Map<ChatChannel, ChannelSender> channelSenders(
            @Qualifier("telegramChannelSender") ChannelSender telegramSender,
            @Qualifier("emailChannelSender") ChannelSender emailSender,
            @Qualifier("vkChannelSender") ChannelSender vkSender,
            @Qualifier("whatsappChannelSender") ChannelSender whatsappSender,
            @Qualifier("dialogXChatChannelSender") ChannelSender dialogXChatSender
    ) {
        Map<ChatChannel, ChannelSender> senders = new EnumMap<>(ChatChannel.class);
        senders.put(ChatChannel.Telegram, telegramSender);
        senders.put(ChatChannel.Email, emailSender);
        senders.put(ChatChannel.VK, vkSender);
        senders.put(ChatChannel.WhatsApp, whatsappSender);
        senders.put(ChatChannel.DialogX_Chat, dialogXChatSender);
        return senders;
    }
}

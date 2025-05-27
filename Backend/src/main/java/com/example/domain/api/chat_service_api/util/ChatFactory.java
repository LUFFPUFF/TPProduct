package com.example.domain.api.chat_service_api.util;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.crm_module.client.Client;
import com.example.domain.api.chat_service_api.model.rest.chat.CreateChatRequestDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ChatFactory {

    public Chat createClientInitiatedChat(Client client, Company company, CreateChatRequestDTO request) {
        Chat chat = new Chat();
        chat.setClient(client);
        chat.setCompany(company);
        chat.setChatChannel(request.getChatChannel());
        chat.setStatus(ChatStatus.PENDING_AUTO_RESPONDER);
        chat.setCreatedAt(LocalDateTime.now());
        chat.setExternalChatId(request.getExternalChatId());
        return chat;
    }

    public Chat createOperatorInitiatedChat(Client client, User operator, ChatChannel channel) {
        Chat chat = new Chat();
        chat.setClient(client);
        chat.setCompany(operator.getCompany());
        chat.setChatChannel(channel);
        chat.setUser(operator);
        chat.setStatus(ChatStatus.ASSIGNED);
        LocalDateTime now = LocalDateTime.now();
        chat.setCreatedAt(now);
        chat.setAssignedAt(now);
        chat.setLastMessageAt(now);
        return chat;
    }
}

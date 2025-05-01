package com.example.domain.api.chat_service_api.service;

import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.crm_module.client.Client;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.domain.dto.chat_module.ChatDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;

    public <T> Optional<T> getClientAndChatChannel(Client entityClient, ChatChannel chatChannel) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Object createChat(ChatDto newChat) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

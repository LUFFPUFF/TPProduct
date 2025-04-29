package com.example.domain.dto.mapper;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.repository.chats_messages_module.ChatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ChatMapperHelper {

    @Autowired
    private ChatRepository chatRepository;

    public Chat mapChatIdToChat(Integer chatId) {
        return chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
    }
}

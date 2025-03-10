package com.example.database.repository.chats_messages_module;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.domain.dto.chat_module.ChatDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Integer> {

    Optional<ChatDto> findByClientAndChatChannel(Integer clientId, String chatChannel);
    Optional<Chat> findByClient(Integer clientId);
}

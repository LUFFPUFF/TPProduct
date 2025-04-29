package com.example.database.repository.chats_messages_module;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.crm_module.client.Client;
import com.example.domain.dto.ChatDto;
import com.example.domain.dto.ClientDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Integer> {

    Optional<Chat> findByClientAndChatChannel(Client client, ChatChannel chatChannel);
    Optional<Client> findByClient(Client client);
}

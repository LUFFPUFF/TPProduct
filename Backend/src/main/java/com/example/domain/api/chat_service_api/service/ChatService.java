package com.example.domain.api.chat_service_api.service;


import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.crm_module.client.Client;
import com.example.domain.dto.chat_module.ChatDto;

import java.util.List;
import java.util.Optional;

public interface ChatService {

    ChatDto createChat(ChatDto chatDto);
    ChatDto getChatById(Integer id);
    List<ChatDto> getAllChats();
    ChatDto updateChat(Integer id, ChatDto chatDto);
    void deleteChat(Integer id);
    Optional<ChatDto> getClientAndChatChannel(Client client, ChatChannel chatChannel);
    Optional<Client> findByClient(Client client);
}

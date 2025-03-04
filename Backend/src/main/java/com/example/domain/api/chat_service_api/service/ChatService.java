package com.example.domain.api.chat_service_api.service;


import com.example.domain.dto.chat_module.ChatDto;

import java.util.List;

public interface ChatService {

    ChatDto createChat(ChatDto chatDto);
    ChatDto getChatById(Integer id);
    List<ChatDto> getAllChats();
    ChatDto updateChat(Integer id, ChatDto chatDto);
    void deleteChat(Integer id);
}

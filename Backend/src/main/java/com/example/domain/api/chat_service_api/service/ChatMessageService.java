package com.example.domain.api.chat_service_api.service;

import com.example.domain.dto.chat_module.MessageDto;

import java.util.List;

public interface ChatMessageService {
    MessageDto createMessage(MessageDto messageDto);
    MessageDto getMessageById(Integer id);
    List<MessageDto> getAllMessages();
    MessageDto updateMessage(Integer id, MessageDto messageDto);
    void deleteMessage(Integer id);
}

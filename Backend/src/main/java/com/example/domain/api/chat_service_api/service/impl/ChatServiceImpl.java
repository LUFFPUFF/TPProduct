package com.example.domain.api.chat_service_api.service.impl;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.domain.dto.chat_module.ChatDto;
import com.example.domain.dto.mapper.MapperDto;
import com.example.domain.api.chat_service_api.service.ChatService;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Data
public class ChatServiceImpl implements ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatServiceImpl.class);

    private final ChatRepository chatRepository;
    private final MapperDto mapperDto;


    @Override
    public ChatDto createChat(ChatDto chatDto) {
        logger.info("Creating chat: {}", chatDto);
        Chat chat = mapperDto.toEntityChat(chatDto);
        Chat savedChat = chatRepository.save(chat);
        return mapperDto.toDtoChat(savedChat);
    }

    @Override
    public ChatDto getChatById(Integer id) {
        logger.info("Fetching chat by id: {}", id);
        Chat chat = chatRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
        return mapperDto.toDtoChat(chat);
    }

    @Cacheable("chats")
    @Override
    public List<ChatDto> getAllChats() {
        logger.info("Fetching all chats");
        return chatRepository.findAll().stream()
                .map(mapperDto::toDtoChat)
                .collect(Collectors.toList());
    }

    @Override
    public ChatDto updateChat(Integer id, ChatDto chatDto) {
        logger.info("Updating chat with id: {}", id);
        Chat existingChat = chatRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
        mapperDto.toEntityChat(chatDto);
        Chat updatedChat = chatRepository.save(existingChat);
        return mapperDto.toDtoChat(updatedChat);
    }

    @Override
    public void deleteChat(Integer id) {
        logger.info("Deleting chat with id: {}", id);
        chatRepository.deleteById(id);
    }
}

package com.example.domain.api.chat_service_api.service.impl;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.crm_module.client.Client;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.database.repository.crm_module.ClientRepository;
import com.example.domain.api.chat_service_api.config.chat.ChatConfig;
import com.example.domain.api.chat_service_api.exception_handler.exception.service.ChatServiceException;
import com.example.domain.dto.chat_module.ChatDto;
import com.example.domain.dto.mapper.MapperDto;
import com.example.domain.api.chat_service_api.service.ChatService;
import jakarta.transaction.Transactional;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final MapperDto mapperDto;
    private final ChatConfig chatConfig;

    @Override
    public ChatDto createChat(ChatDto chatDto) {
        try {
            log.info("Creating chat: {}", chatDto);
            Chat chat = mapperDto.toEntityChat(chatDto);
            Chat savedChat = chatRepository.save(chat);
            return mapperDto.toDtoChat(savedChat);
        } catch (Exception e) {
            log.error("Error while creating chat: {}", e.getMessage(), e);
            throw new ChatServiceException("Failed to create chat: " + e.getMessage(), e);
        }
    }

    @Override
    public ChatDto getChatById(Integer id) {
        try {
            log.info("Fetching chat by id: {}", id);
            Chat chat = chatRepository.findById(id)
                    .orElseThrow(() -> new ChatServiceException("Chat not found with id: " + id));
            return mapperDto.toDtoChat(chat);
        } catch (Exception e) {
            log.error("Error while fetching chat by id {}: {}", id, e.getMessage(), e);
            throw new ChatServiceException("Failed to fetch chat by id " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<ChatDto> getClientAndChatChannel(Client client, ChatChannel chatChannel) {
        try {
            log.info("Fetching chat by client and chat channel: {}, {}", client, chatChannel);
            return chatRepository.findByClientAndChatChannel(client, chatChannel)
                    .map(mapperDto::toDtoChat);
        } catch (Exception e) {
            log.error("Error while fetching chat by client and chat channel: {}, {}", client, chatChannel, e);
            throw new ChatServiceException("Failed to fetch chat by client and chat channel: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Client> findByClient(Client client) {
        try {
            log.info("Fetching client: {}", client);
            return chatRepository.findByClient(client);
        } catch (Exception e) {
            log.error("Error while fetching client: {}", client, e);
            throw new ChatServiceException("Failed to fetch client: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ChatDto> getAllChats() {
        try {
            log.info("Fetching all chats");
            return chatRepository.findAll().stream()
                    .map(mapperDto::toDtoChat)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error while fetching all chats: {}", e.getMessage(), e);
            throw new ChatServiceException("Failed to fetch all chats: " + e.getMessage(), e);
        }
    }

    @Override
    public ChatDto updateChat(Integer id, ChatDto chatDto) {
        try {
            log.info("Updating chat with id: {}", id);
            Chat existingChat = chatRepository.findById(id)
                    .orElseThrow(() -> new ChatServiceException("Chat not found with id: " + id));
            mapperDto.toEntityChat(chatDto);
            Chat updatedChat = chatRepository.save(existingChat);
            return mapperDto.toDtoChat(updatedChat);
        } catch (Exception e) {
            log.error("Error while updating chat with id {}: {}", id, e.getMessage(), e);
            throw new ChatServiceException("Failed to update chat with id " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteChat(Integer id) {
        try {
            log.info("Deleting chat with id: {}", id);
            chatRepository.deleteById(id);
        } catch (Exception e) {
            log.error("Error while deleting chat with id {}: {}", id, e.getMessage(), e);
            throw new ChatServiceException("Failed to delete chat with id " + id + ": " + e.getMessage(), e);
        }
    }
}

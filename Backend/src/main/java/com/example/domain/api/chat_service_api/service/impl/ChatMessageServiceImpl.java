package com.example.domain.api.chat_service_api.service.impl;

import com.example.database.model.chats_messages_module.ChatMessage;
import com.example.database.repository.chats_messages_module.ChatMessageRepository;
import com.example.domain.api.chat_service_api.config.chat.ChatConfig;
import com.example.domain.api.chat_service_api.exception_handler.exception.service.ChatMessageServiceException;
import com.example.domain.api.chat_service_api.service.ChatMessageService;
import com.example.domain.dto.MessageDto;
import com.example.domain.dto.mapper.MapperDto;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final MapperDto mapperDto;
    private final ChatConfig config;

    @Override
    public MessageDto createMessage(@Valid MessageDto messageDto) {
        try {
            if (messageDto.getContent().length() > config.getMaxMessageLength()) {
                throw new ChatMessageServiceException("Message content exceeds maximum allowed length");
            }

            ChatMessage chatMessage = mapperDto.toEntityChatMessage(messageDto);
            ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

            return mapperDto.toDtoChatMessage(savedMessage);
        } catch (ChatMessageServiceException e) {
            log.error("Validation error while creating message: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Error while creating message: {}", e.getMessage(), e);
            throw new ChatMessageServiceException("Failed to create message: " + e.getMessage(), e);
        }
    }

    @Override
    public MessageDto getMessageById(Integer id) {
        try {
            log.info("Fetching message by id: {}", id);
            ChatMessage chatMessage = chatMessageRepository.findById(id)
                    .orElseThrow(() -> new ChatMessageServiceException("Message not found with id: " + id));
            return mapperDto.toDtoChatMessage(chatMessage);
        } catch (Exception e) {
            log.error("Error while fetching message by id {}: {}", id, e.getMessage(), e);
            throw new ChatMessageServiceException("Failed to fetch message by id " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<MessageDto> getAllMessages() {
        try {
            log.info("Fetching all messages");
            return chatMessageRepository.findAll().stream()
                    .map(mapperDto::toDtoChatMessage)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error while fetching all messages: {}", e.getMessage(), e);
            throw new ChatMessageServiceException("Failed to fetch all messages: " + e.getMessage(), e);
        }
    }

    @Override
    public MessageDto updateMessage(Integer id, MessageDto messageDto) {
        try {
            log.info("Updating message with id: {}", id);
            ChatMessage existingMessage = chatMessageRepository.findById(id)
                    .orElseThrow(() -> new ChatMessageServiceException("Message not found with id: " + id));

            existingMessage.setContent(messageDto.getContent());

            ChatMessage updatedMessage = chatMessageRepository.save(existingMessage);
            return mapperDto.toDtoChatMessage(updatedMessage);
        } catch (Exception e) {
            log.error("Error while updating message with id {}: {}", id, e.getMessage(), e);
            throw new ChatMessageServiceException("Failed to update message with id " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteMessage(Integer id) {
        try {
            log.info("Deleting message with id: {}", id);
            chatMessageRepository.deleteById(id);
        } catch (Exception e) {
            log.error("Error while deleting message with id {}: {}", id, e.getMessage(), e);
            throw new ChatMessageServiceException("Failed to delete message with id " + id + ": " + e.getMessage(), e);
        }
    }
}

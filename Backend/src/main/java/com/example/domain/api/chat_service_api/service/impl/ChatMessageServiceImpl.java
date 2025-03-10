package com.example.domain.api.chat_service_api.service.impl;

import com.example.database.model.chats_messages_module.ChatMessage;
import com.example.database.repository.chats_messages_module.ChatMessageRepository;
import com.example.domain.api.chat_service_api.service.ChatMessageService;
import com.example.domain.dto.chat_module.MessageDto;
import com.example.domain.dto.mapper.MapperDto;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Data
@Transactional
public class ChatMessageServiceImpl implements ChatMessageService {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageServiceImpl.class);

    private final ChatMessageRepository chatMessageRepository;
    private final MapperDto mapperDto;

    @Override
    public MessageDto createMessage(@Valid MessageDto messageDto) {
        try {
            ChatMessage chatMessage = mapperDto.toEntityChatMessage(messageDto);

            ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

            return mapperDto.toDtoChatMessage(savedMessage);
        } catch (Exception e) {
            System.out.println("Error while creating message: " + e);
            throw e;
        }
    }

    @Override
    public MessageDto getMessageById(Integer id) {
        logger.info("Fetching message by id: {}", id);
        ChatMessage chatMessage = chatMessageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        return mapperDto.toDtoChatMessage(chatMessage);
    }

    @Override
    public List<MessageDto> getAllMessages() {
        logger.info("Fetching all messages");
        return chatMessageRepository.findAll().stream()
                .map(mapperDto::toDtoChatMessage)
                .collect(Collectors.toList());
    }

    @Override
    public MessageDto updateMessage(Integer id, MessageDto messageDto) {
        logger.info("Updating message with id: {}", id);
        ChatMessage existingMessage = chatMessageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        existingMessage.setContent(messageDto.getContent());

        ChatMessage updatedMessage = chatMessageRepository.save(existingMessage);
        return mapperDto.toDtoChatMessage(updatedMessage);
    }

    @Override
    public void deleteMessage(Integer id) {
        logger.info("Deleting message with id: {}", id);
        chatMessageRepository.deleteById(id);
    }
}

package com.example.domain.api.chat_service_api.service.impl;

import com.example.database.model.chats_messages_module.ChatMessage;
import com.example.database.repository.chats_messages_module.ChatMessageRepository;
import com.example.domain.api.chat_service_api.service.ChatMessageService;
import com.example.domain.dto.MessageDto;
import com.example.domain.api.chat_service_api.mapper.ChatMapper;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Data
public class ChatMessageServiceImpl implements ChatMessageService {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageServiceImpl.class);

    private final ChatMessageRepository chatMessageRepository;
    private final ChatMapper chatMapper;

    @Override
    @Transactional
    public MessageDto createMessage(@Valid MessageDto messageDto) {
        logger.info("Creating message: {}", messageDto);
        ChatMessage chatMessage = chatMapper.toEntityChatMessage(messageDto);
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        return chatMapper.toDtoChatMessage(savedMessage);
    }

    @Override
    public MessageDto getMessageById(Integer id) {
        logger.info("Fetching message by id: {}", id);
        ChatMessage chatMessage = chatMessageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        return chatMapper.toDtoChatMessage(chatMessage);
    }

    @Override
    public List<MessageDto> getAllMessages() {
        logger.info("Fetching all messages");
        return chatMessageRepository.findAll().stream()
                .map(chatMapper::toDtoChatMessage)
                .collect(Collectors.toList());
    }

    @Override
    public MessageDto updateMessage(Integer id, MessageDto messageDto) {
        logger.info("Updating message with id: {}", id);
        ChatMessage existingMessage = chatMessageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        chatMapper.toEntityChatMessage(messageDto);
        ChatMessage updatedMessage = chatMessageRepository.save(existingMessage);
        return chatMapper.toDtoChatMessage(updatedMessage);
    }

    @Override
    public void deleteMessage(Integer id) {
        logger.info("Deleting message with id: {}", id);
        chatMessageRepository.deleteById(id);
    }
}

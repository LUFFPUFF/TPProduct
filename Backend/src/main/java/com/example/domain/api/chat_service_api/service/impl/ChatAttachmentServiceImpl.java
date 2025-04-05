package com.example.domain.api.chat_service_api.service.impl;

import com.example.database.model.chats_messages_module.ChatAttachment;
import com.example.database.repository.chats_messages_module.ChatAttachmentRepository;
import com.example.domain.api.chat_service_api.config.FileUploadConfig;
import com.example.domain.dto.ChatAttachmentDto;
import com.example.domain.api.chat_service_api.service.ChatAttachmentService;
import com.example.domain.api.chat_service_api.service.file_service.AntivirusService;
import com.example.domain.api.chat_service_api.service.file_service.FileUploadService;
import com.example.domain.dto.mapper.MapperDto;
import com.example.domain.exception_handler.chat_module.ChatAttachmentException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@Data
@Slf4j
public class ChatAttachmentServiceImpl implements ChatAttachmentService {

    private final ChatAttachmentRepository chatAttachmentRepository;
    private final MapperDto mapperDto;
    private final AntivirusService antivirusService;
    private final FileUploadService fileUploadService;
    private final FileUploadConfig fileUploadConfig;

    @Override
    @Async
    public ChatAttachmentDto createAttachment(ChatAttachmentDto attachmentDto) throws ChatAttachmentException {
        log.info("Creating attachment: {}", attachmentDto);
        Path filePath = Path.of(fileUploadConfig.getLocation(), attachmentDto.getFileUrl());

        try {
            if (!antivirusService.scanFile(filePath).get()) {
                throw new ChatAttachmentException("File is infected");
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new ChatAttachmentException("Error during antivirus scan", e);
        }

        fileUploadService.validateFile(filePath);

        ChatAttachment chatAttachment = mapperDto.toEntityChatAttachment(attachmentDto);
        ChatAttachment savedAttachment = chatAttachmentRepository.save(chatAttachment);
        return mapperDto.toDtoChatAttachment(savedAttachment);
    }

    @Override
    public ChatAttachmentDto getAttachmentById(Integer id) {
        log.info("Fetching attachment by id: {}", id);
        ChatAttachment chatAttachment = chatAttachmentRepository.findById(id)
                .orElseThrow(() -> new ChatAttachmentException("Attachment not found"));
        return mapperDto.toDtoChatAttachment(chatAttachment);
    }

    @Override
    public List<ChatAttachmentDto> getAllAttachments() {
        log.info("Fetching all attachments");
        return chatAttachmentRepository.findAll().stream()
                .map(mapperDto::toDtoChatAttachment)
                .collect(Collectors.toList());
    }

    @Override
    public ChatAttachmentDto updateAttachment(Integer id, ChatAttachmentDto attachmentDto) {
        log.info("Updating attachment with id: {}", id);
        ChatAttachment existingAttachment = chatAttachmentRepository.findById(id)
                .orElseThrow(() -> new ChatAttachmentException("Attachment not found"));
        mapperDto.toEntityChatAttachment(attachmentDto);
        ChatAttachment updatedAttachment = chatAttachmentRepository.save(existingAttachment);
        return mapperDto.toDtoChatAttachment(updatedAttachment);
    }

    @Override
    public void deleteAttachment(Integer id) {
        log.info("Deleting attachment with id: {}", id);
        chatAttachmentRepository.deleteById(id);
    }
}

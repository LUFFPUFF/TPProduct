package com.example.domain.api.chat_service_api.service.impl;

import com.example.database.model.chats_messages_module.ChatAttachment;
import com.example.database.repository.chats_messages_module.ChatAttachmentRepository;
import com.example.domain.api.chat_service_api.config.FileUploadConfig;
import com.example.domain.api.chat_service_api.exception_handler.exception.service.AntivirusException;
import com.example.domain.dto.ChatAttachmentDto;
import com.example.domain.api.chat_service_api.service.ChatAttachmentService;
import com.example.domain.api.chat_service_api.service.file_service.AntivirusService;
import com.example.domain.api.chat_service_api.service.file_service.FileUploadService;
import com.example.domain.dto.mapper.MapperDto;
import com.example.domain.api.chat_service_api.exception_handler.exception.service.ChatAttachmentServiceException;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatAttachmentServiceImpl implements ChatAttachmentService {

    private final ChatAttachmentRepository chatAttachmentRepository;
    private final MapperDto mapperDto;
    private final AntivirusService antivirusService;
    private final FileUploadService fileUploadService;
    private final FileUploadConfig fileUploadConfig;

    @Override
    public ChatAttachmentDto createAttachment(@Valid ChatAttachmentDto attachmentDto) throws ChatAttachmentServiceException {
        try {
            log.info("Creating attachment: {}", attachmentDto);
            Path filePath = Path.of(fileUploadConfig.getLocation(), attachmentDto.getFileUrl());

            if (!antivirusService.scanFile(filePath).get()) {
                throw new AntivirusException("File is infected");
            }

            fileUploadService.validateFile(filePath);

            ChatAttachment chatAttachment = mapperDto.toEntityChatAttachment(attachmentDto);
            ChatAttachment savedAttachment = chatAttachmentRepository.save(chatAttachment);
            return mapperDto.toDtoChatAttachment(savedAttachment);
        } catch (AntivirusException e) {
            log.error("Antivirus scan failed for file: {}", attachmentDto.getFileUrl(), e);
            throw new ChatAttachmentServiceException("Antivirus scan failed: " + e.getMessage(), e);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error during antivirus scan for file: {}", attachmentDto.getFileUrl(), e);
            throw new ChatAttachmentServiceException("Error during antivirus scan: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error while creating attachment: {}", e.getMessage(), e);
            throw new ChatAttachmentServiceException("Failed to create attachment: " + e.getMessage(), e);
        }
    }

    @Override
    public ChatAttachmentDto getAttachmentById(Integer id) {
        try {
            log.info("Fetching attachment by id: {}", id);
            ChatAttachment chatAttachment = chatAttachmentRepository.findById(id)
                    .orElseThrow(() -> new ChatAttachmentServiceException("Attachment not found with id: " + id));
            return mapperDto.toDtoChatAttachment(chatAttachment);
        } catch (Exception e) {
            log.error("Error while fetching attachment by id {}: {}", id, e.getMessage(), e);
            throw new ChatAttachmentServiceException("Failed to fetch attachment by id " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<ChatAttachmentDto> getAllAttachments() {
        try {
            log.info("Fetching all attachments");
            return chatAttachmentRepository.findAll().stream()
                    .map(mapperDto::toDtoChatAttachment)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error while fetching all attachments: {}", e.getMessage(), e);
            throw new ChatAttachmentServiceException("Failed to fetch all attachments: " + e.getMessage(), e);
        }
    }

    @Override
    public ChatAttachmentDto updateAttachment(Integer id, ChatAttachmentDto attachmentDto) {
        try {
            log.info("Updating attachment with id: {}", id);
            ChatAttachment existingAttachment = chatAttachmentRepository.findById(id)
                    .orElseThrow(() -> new ChatAttachmentServiceException("Attachment not found with id: " + id));

            mapperDto.toEntityChatAttachment(attachmentDto);
            ChatAttachment updatedAttachment = chatAttachmentRepository.save(existingAttachment);
            return mapperDto.toDtoChatAttachment(updatedAttachment);
        } catch (Exception e) {
            log.error("Error while updating attachment with id {}: {}", id, e.getMessage(), e);
            throw new ChatAttachmentServiceException("Failed to update attachment with id " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteAttachment(Integer id) {
        try {
            log.info("Deleting attachment with id: {}", id);
            chatAttachmentRepository.deleteById(id);
        } catch (Exception e) {
            log.error("Error while deleting attachment with id {}: {}", id, e.getMessage(), e);
            throw new ChatAttachmentServiceException("Failed to delete attachment with id " + id + ": " + e.getMessage(), e);
        }
    }
}

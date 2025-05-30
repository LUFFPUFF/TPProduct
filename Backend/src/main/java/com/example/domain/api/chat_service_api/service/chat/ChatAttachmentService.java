
package com.example.domain.api.chat_service_api.service.chat;

import com.example.domain.dto.ChatAttachmentDto;

import java.util.List;

public interface ChatAttachmentService {
    ChatAttachmentDto createAttachment(ChatAttachmentDto attachmentDto);
    ChatAttachmentDto getAttachmentById(Integer id);
    List<ChatAttachmentDto> getAllAttachments();
    ChatAttachmentDto updateAttachment(Integer id, ChatAttachmentDto attachmentDto);
    void deleteAttachment(Integer id);
}
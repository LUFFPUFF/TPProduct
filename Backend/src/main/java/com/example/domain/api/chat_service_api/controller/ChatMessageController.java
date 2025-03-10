package com.example.domain.api.chat_service_api.controller;

import com.example.domain.dto.chat_module.MessageDto;
import com.example.domain.api.chat_service_api.service.ChatMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @GetMapping("/{id}")
    public ResponseEntity<MessageDto> getMessageById(@PathVariable Integer id) {
        try {
            MessageDto messageDto = chatMessageService.getMessageById(id);
            return ResponseEntity.ok(messageDto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping
    public ResponseEntity<List<MessageDto>> getAllMessages() {
        List<MessageDto> messages = chatMessageService.getAllMessages();
        return ResponseEntity.ok(messages);
    }
}

package com.example.domain.api.chat_service_api.controller;

import com.example.domain.dto.chat_module.ChatDto;
import com.example.domain.api.chat_service_api.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatDto> createChat(@RequestBody ChatDto chatDto) {
        ChatDto createdChat = chatService.createChat(chatDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdChat);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChatDto> getChatById(@PathVariable Integer id) {
        try {
            ChatDto chatDto = chatService.getChatById(id);
            return ResponseEntity.ok(chatDto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping
    public ResponseEntity<List<ChatDto>> getAllChats() {
        List<ChatDto> chats = chatService.getAllChats();
        return ResponseEntity.ok(chats);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChatDto> updateChat(@PathVariable Integer id, @RequestBody ChatDto chatDto) {
        try {
            ChatDto updatedChat = chatService.updateChat(id, chatDto);
            return ResponseEntity.ok(updatedChat);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChat(@PathVariable Integer id) {
        chatService.deleteChat(id);
        return ResponseEntity.noContent().build();
    }
}

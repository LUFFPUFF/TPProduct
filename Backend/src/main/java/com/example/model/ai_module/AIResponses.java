package com.example.model.ai_module;

import com.example.model.chats_messages_module.ChatMessage;
import com.example.model.chats_messages_module.chat.Chat;
import com.example.model.crm_module.client.Client;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_responses", indexes = {
        @Index(name = "idx_ai_responses_chat_message_id", columnList = "chat_message_id"),
        @Index(name = "idx_ai_responses_client_id", columnList = "client_id"),
        @Index(name = "idx_ai_responses_chat_id", columnList = "chat_id")
})
@Data
public class AIResponses {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "chat_message_id", referencedColumnName = "id", nullable = false)
    private ChatMessage chatMessage;

    @ManyToOne
    @JoinColumn(name = "client_id", referencedColumnName = "id", nullable = false)
    private Client client;

    @ManyToOne
    @JoinColumn(name = "chat_id", referencedColumnName = "id", nullable = false)
    private Chat chat;

    @Column(name = "response_text")
    private String responsesText;

    @Column(name = "confidence")
    private Float confidence;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

package com.example.model.chats_messages_module;

import com.example.model.chats_messages_module.chat.Chat;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_messages_chat_id", columnList = "chat_id"),
        @Index(name = "idx_chat_messages_participant_id", columnList = "participant_id"),
        @Index(name = "idx_chat_messages_sent_at", columnList = "sent_at")
})
@Data
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "chat_id", referencedColumnName = "id", nullable = false)
    private Chat chat;

    @Column(name = "content")
    private String content;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;
}

package com.example.model.chats_messages_module;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "chat_attachments", indexes = {
        @Index(name = "idx_chat_attachments_chat_message_id", columnList = "chat_message_id"),
        @Index(name = "idx_chat_attachments_file_type", columnList = "file_type")
})
@Data
public class ChatAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "chat_message_id", referencedColumnName = "id", nullable = false)
    private ChatMessage chatMessage;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "file_type", length = 50, nullable = false)
    private String fileType;
}

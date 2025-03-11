package com.example.database.model.chats_messages_module.chat;

import com.example.database.model.chats_messages_module.ChatMessage;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.crm_module.client.Client;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chats", indexes = {
        @Index(name = "idx_chats_status", columnList = "status"),
        @Index(name = "idx_chats_created_at", columnList = "created_at")
})
@Data
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "chat_channel", nullable = false)
    private ChatChannel chatChannel;

    @Column(name = "status", length = 50, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Override
    public String toString() {
        return "Chat{" +
                "id=" + id +
                ", client=" + client +
                ", user=" + user +
                ", chatChannel=" + chatChannel +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

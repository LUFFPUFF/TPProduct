package com.example.database.model.chats_messages_module.chat;

import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.crm_module.client.Client;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "chat_channel", nullable = false)
    private ChatChannel chatChannel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private ChatStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "assigned_at", nullable = true)
    private LocalDateTime assignedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "last_message_at", nullable = true)
    private LocalDateTime lastMessageAt;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sentAt ASC")
    private List<ChatMessage> messages;

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

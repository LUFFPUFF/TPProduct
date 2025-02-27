package com.example.model.chats_messages_module.chat;

import com.example.model.company_subscription_module.user_roles.user.User;
import com.example.model.crm_modle.client.Client;
import jakarta.persistence.*;

import java.sql.Date;

@Entity
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "chat_channel", nullable = false)
    private ChatChannel chatChannel;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "status", length = 50, nullable = false)
    private String status;
}

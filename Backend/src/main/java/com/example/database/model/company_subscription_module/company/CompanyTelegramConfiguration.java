package com.example.database.model.company_subscription_module.company;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "company_telegram_configuration", indexes = {
        @Index(name = "idx_telegram_company_id", columnList = "company_id")
})
@Data
public class CompanyTelegramConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "chat_telegram_id")
    private Long chatTelegramId;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "bot_username", nullable = false)
    private String botUsername;

    @Column(name = "bot_token", nullable = false)
    private String botToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

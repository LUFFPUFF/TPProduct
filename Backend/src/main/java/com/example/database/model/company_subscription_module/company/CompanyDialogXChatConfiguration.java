package com.example.database.model.company_subscription_module.company;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "company_dialogx_chat_configuration", indexes = {
        @Index(name = "idx_dialogx_chat_config_company_id", columnList = "company_id", unique = true), // Assuming one config per company
        @Index(name = "idx_dialogx_chat_config_widget_id", columnList = "widget_id", unique = true)
})
@Data
@NoArgsConstructor
public class CompanyDialogXChatConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false, unique = true)
    private Company company;

    @Column(name = "widget_id", nullable = false, unique = true, updatable = false)
    private String widgetId;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "welcome_message", length = 500)
    private String welcomeMessage;

    @Column(name = "theme_color", length = 20)
    private String themeColor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (widgetId == null) {
            widgetId = UUID.randomUUID().toString();
        }

        if (welcomeMessage == null) {
            welcomeMessage = "Привет! Чем могу помочь?";
        }
        if (themeColor == null) {
            themeColor = "#5A38D9";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

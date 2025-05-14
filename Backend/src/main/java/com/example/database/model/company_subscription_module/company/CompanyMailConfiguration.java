package com.example.database.model.company_subscription_module.company;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "company_mail_configuration", indexes = {
        @Index(name = "idx_mail_company_id", columnList = "company_id")
})
@Data
public class CompanyMailConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "email_address", nullable = false, unique = true)
    private String emailAddress;

    @Column(name = "app_password", nullable = false)
    private String appPassword;

    @Column(name = "imap_server", nullable = false)
    private String imapServer;

    @Column(name = "smtp_server")
    private String smtpServer;

    @Column(name = "imap_port", nullable = false)
    private Integer imapPort = 993;

    @Column(name = "folder")
    private String folder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

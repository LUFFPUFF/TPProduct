package com.example.database.model.company_subscription_module.company;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "company_gmail_configuration", indexes = {
        @Index(name = "idx_gmail_company_id", columnList = "company_id")
})
@Data
public class CompanyGmailConfiguration {

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
    private String imapServer = "imap.gmail.com";

    @Column(name = "imap_port", nullable = false)
    private Integer imapPort = 993;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

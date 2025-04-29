package com.example.database.model.integration_module;

import com.example.database.model.company_subscription_module.company.Company;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "integrations", indexes = {
        @Index(name = "idx_integrations_service_name", columnList = "service_name"),
        @Index(name = "idx_integrations_status", columnList = "status")
})
@Data
public class Integration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "company_id", referencedColumnName = "id")
    private Company company;

    @Column(name = "service_name")
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private IntegrationStatus status;

    @Column(name = "setting_key")
    private String settingKey;

    @Column(name = "setting_value")
    private String settingValue;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

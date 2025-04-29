package com.example.database.model.company_subscription_module.subscription;

import com.example.database.model.company_subscription_module.company.Company;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription", indexes = {
        @Index(name = "idx_subscription_company_id", columnList = "company_id"),
        @Index(name = "idx_subscription_status", columnList = "status"),
        @Index(name = "idx_subscription_start_subscription", columnList = "start_subscription"),
        @Index(name = "idx_subscription_end_subscription", columnList = "end_subscription")
})
@Data
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "company_id", referencedColumnName = "id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SubscriptionStatus status;

    @Column(nullable = false)
    private float cost;

    @Column(nullable = false)
    private int countOperators;

    @Column(nullable = false)
    private int maxOperators;

    @Column(nullable = false)
    private LocalDateTime startSubscription;

    @Column(nullable = false)
    private LocalDateTime endSubscription;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

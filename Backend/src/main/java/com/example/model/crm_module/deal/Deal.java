package com.example.model.crm_module.deal;

import com.example.model.company_subscription_module.user_roles.user.User;
import com.example.model.crm_module.client.Client;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "deals", indexes = {
        @Index(name = "idx_deals_client_id", columnList = "client_id"),
        @Index(name = "idx_deals_user_id", columnList = "user_id"),
        @Index(name = "idx_deals_created_at", columnList = "created_at"),
        @Index(name = "idx_deals_stage_id", columnList = "stage_id")
})
@Data
public class Deal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "stage_id")
    private DealStage stage;

    @Column(name = "content")
    private String content;

    @Column(name = "amount")
    private Float amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DealStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;



}

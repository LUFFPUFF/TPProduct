package com.example.database.model.ai_module;

import com.example.database.model.crm_module.client.Client;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_feedback", indexes = {
        @Index(name = "idx_ai_feedback_ai_response_id", columnList = "ai_response_id"),
        @Index(name = "idx_ai_feedback_client_id", columnList = "client_id")
})
@Data
public class AIFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "ai_response_id", referencedColumnName = "id", nullable = false)
    private AIResponses responses;

    @ManyToOne
    @JoinColumn(name = "client_id", referencedColumnName = "id", nullable = false)
    private Client client;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "comment")
    private String comment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

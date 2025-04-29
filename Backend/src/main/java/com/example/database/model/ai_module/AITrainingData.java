package com.example.database.model.ai_module;

import com.example.database.model.company_subscription_module.user_roles.user.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_training_data", indexes = {
        @Index(name = "idx_ai_training_data_user_id", columnList = "user_id"),
        @Index(name = "idx_ai_training_data_category", columnList = "category")
})
@Data
public class AITrainingData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @Column(name = "question")
    private String question;

    @Column(name = "answer")
    private String answer;

    @Column(name = "category")
    private String category;

    @Column(name = "source")
    private String source;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

}

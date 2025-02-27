package com.example.model.ai_module;

import com.example.model.company_subscription_module.user_roles.user.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "predefined_answers", indexes = {
        @Index(name = "idx_predefined_answers_user_id", columnList = "user_id"),
        @Index(name = "idx_predefined_answers_category", columnList = "category")
})
@Data
public class PredefinedAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @Column(name = "category")
    private String category;

    @Column(name = "title")
    private String title;

    @Column(name = "answer")
    private String answer;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

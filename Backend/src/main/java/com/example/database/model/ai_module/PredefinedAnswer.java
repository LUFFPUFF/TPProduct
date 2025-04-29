package com.example.database.model.ai_module;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.domain.api.ans_api_module.answer_finder.domain.TrustScore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "predefined_answers", indexes = {
        @Index(name = "idx_predefined_answers_category", columnList = "category")
})
@Data
public class PredefinedAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "company_id", referencedColumnName = "id", nullable = false)
    private Company company;

    @Column(name = "category")
    private String category;

    @Column(name = "title")
    private String title;

    @Column(name = "answer")
    private String answer;

    @Embedded
    @Column(name = "trust_score", nullable = false)
    private TrustScore trustScore;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
}

package com.example.repository.ai_module;

import com.example.model.ai_module.AIFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AIFeedbackRepository extends JpaRepository<AIFeedback, Integer> {
}

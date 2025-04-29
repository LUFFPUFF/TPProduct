package com.example.database.repository.ai_module;

import com.example.database.model.ai_module.AIFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AIFeedbackRepository extends JpaRepository<AIFeedback, Integer> {
}

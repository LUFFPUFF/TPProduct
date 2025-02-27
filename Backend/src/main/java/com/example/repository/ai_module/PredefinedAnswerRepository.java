package com.example.repository.ai_module;

import com.example.model.ai_module.PredefinedAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PredefinedAnswerRepository extends JpaRepository<PredefinedAnswer, Integer> {
}

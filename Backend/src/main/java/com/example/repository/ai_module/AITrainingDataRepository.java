package com.example.repository.ai_module;

import com.example.model.ai_module.AITrainingData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AITrainingDataRepository extends JpaRepository<AITrainingData, Integer> {
}

package com.example.database.repository.ai_module;

import com.example.database.model.ai_module.AIResponses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AIResponsesRepository extends JpaRepository<AIResponses, Integer> {

    Optional<AIResponses> findTopByChatIdOrderByCreatedAtDesc(Integer chatId);
}

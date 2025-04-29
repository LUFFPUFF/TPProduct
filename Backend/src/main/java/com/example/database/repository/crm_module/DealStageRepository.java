package com.example.database.repository.crm_module;

import com.example.database.model.crm_module.deal.DealStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DealStageRepository extends JpaRepository<DealStage, Integer> {
}

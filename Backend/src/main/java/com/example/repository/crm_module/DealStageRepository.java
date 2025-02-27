package com.example.repository.crm_module;

import com.example.model.crm_module.deal.DealStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DealStageRepository extends JpaRepository<DealStage, Integer> {
}

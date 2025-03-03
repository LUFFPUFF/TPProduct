package com.example.database.repository.crm_module;

import com.example.database.model.crm_module.deal.Deal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DealRepository extends JpaRepository<Deal, Integer> {
}

package com.example.database.repository.company_subscription_module;

import com.example.database.model.company_subscription_module.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Integer> {
}

package com.example.repository.company_subscription_module;

import com.example.model.company_subscription_module.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Integer> {
}

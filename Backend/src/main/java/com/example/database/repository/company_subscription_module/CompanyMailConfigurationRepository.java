package com.example.database.repository.company_subscription_module;

import com.example.database.model.company_subscription_module.company.CompanyMailConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyMailConfigurationRepository extends JpaRepository<CompanyMailConfiguration, Integer> {

    Optional<CompanyMailConfiguration> findByEmailAddress(String emailAddress);
    List<CompanyMailConfiguration> findByCompanyId(Integer companyId);
}

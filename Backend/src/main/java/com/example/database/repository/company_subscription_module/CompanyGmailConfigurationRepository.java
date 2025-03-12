package com.example.database.repository.company_subscription_module;

import com.example.database.model.company_subscription_module.company.CompanyGmailConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyGmailConfigurationRepository extends JpaRepository<CompanyGmailConfiguration, Integer> {

    Optional<CompanyGmailConfiguration> findByEmailAddress(String emailAddress);
}

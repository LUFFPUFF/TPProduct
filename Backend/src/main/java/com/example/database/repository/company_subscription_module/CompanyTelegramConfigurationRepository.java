package com.example.database.repository.company_subscription_module;

import com.example.database.model.company_subscription_module.company.CompanyTelegramConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyTelegramConfigurationRepository extends JpaRepository<CompanyTelegramConfiguration, Integer> {

    Optional<CompanyTelegramConfiguration> findByBotUsername(String botUsername);

    Optional<CompanyTelegramConfiguration> findByCompanyId(Integer companyId);

    List<CompanyTelegramConfiguration> findAllByCompanyId(Integer companyId);
}

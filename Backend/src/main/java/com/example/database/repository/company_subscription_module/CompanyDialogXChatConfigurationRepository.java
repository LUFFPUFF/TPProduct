package com.example.database.repository.company_subscription_module;

import com.example.database.model.company_subscription_module.company.CompanyDialogXChatConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyDialogXChatConfigurationRepository extends JpaRepository<CompanyDialogXChatConfiguration, Integer> {

    Optional<CompanyDialogXChatConfiguration> findByCompanyId(Integer companyId);
    Optional<CompanyDialogXChatConfiguration> findByWidgetId(String widgetId);
    List<CompanyDialogXChatConfiguration> findAllByEnabledTrue();
}

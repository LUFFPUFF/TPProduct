package com.example.database.repository.chats_messages_module;

import com.example.database.model.company_subscription_module.company.CompanyTelegramConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyTelegramConfigurationRepository extends JpaRepository<CompanyTelegramConfiguration, Integer> {

    Optional<CompanyTelegramConfiguration> findByBotUsername(String botUsername);
}

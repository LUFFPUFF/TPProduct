package com.example.database.repository.company_subscription_module;

import com.example.database.model.company_subscription_module.company.CompanyWhatsappConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyWhatsappConfigurationRepository extends JpaRepository<CompanyWhatsappConfiguration, Integer> {

    int countByAccessTokenIsNotNullAndAccessTokenIsNot(String emptyToken);

    List<CompanyWhatsappConfiguration> findAllByAccessTokenIsNotNullAndAccessTokenIsNot(String emptyToken);

    Optional<CompanyWhatsappConfiguration> findByCompanyIdAndAccessTokenIsNotNullAndAccessTokenIsNot(Integer companyId, String emptyToken);

    Optional<CompanyWhatsappConfiguration> findByPhoneNumberId(Long phoneNumberId);
}

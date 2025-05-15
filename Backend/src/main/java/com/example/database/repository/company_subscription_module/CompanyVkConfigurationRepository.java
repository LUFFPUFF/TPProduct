package com.example.database.repository.company_subscription_module;

import com.example.database.model.company_subscription_module.company.CompanyVkConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyVkConfigurationRepository extends JpaRepository<CompanyVkConfiguration, Integer> {

    int countByAccessTokenIsNotNullAndAccessTokenIsNot(String emptyToken);

    List<CompanyVkConfiguration> findAllByAccessTokenIsNotNullAndAccessTokenIsNot(String emptyToken);

    Optional<CompanyVkConfiguration> findByCompanyIdAndAccessTokenIsNotNullAndAccessTokenIsNot(Integer companyId, String emptyToken);

    Optional<CompanyVkConfiguration> findByCommunityId(Long communityId);
}

package com.example.domain.api.company_module.service.impl;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.repository.company_subscription_module.CompanyRepository;
import com.example.domain.api.authentication_module.service.interfaces.CurrentUserDataService;
import com.example.domain.api.company_module.dto.ChangeCompanyDataDto;
import com.example.domain.api.company_module.service.CompanySettingsService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompanySettingsServiceImpl implements CompanySettingsService {
    private final CompanyRepository companyRepository;
    private final CurrentUserDataService currentUserDataService;


    @Override
    @Transactional
    public void changeCompanyData(ChangeCompanyDataDto changeCompanyDataDto) {
        Company company =  currentUserDataService.getUserCompany();
        company.setName(changeCompanyDataDto.getName());
        company.setCompanyDescription(changeCompanyDataDto.getDescription());
        companyRepository.save(company);
    }


}

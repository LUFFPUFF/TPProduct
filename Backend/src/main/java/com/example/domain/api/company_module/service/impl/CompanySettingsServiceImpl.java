package com.example.domain.api.company_module.service.impl;

import com.example.database.repository.company_subscription_module.CompanyRepository;
import com.example.domain.api.company_module.service.CompanySettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompanySettingsServiceImpl implements CompanySettingsService {
    private final CompanyRepository companyRepository;
    @Override
    public String changeName(String name) {
        return null;
    }

    @Override
    public String changeOwner(String email) {
        return "";
    }
}

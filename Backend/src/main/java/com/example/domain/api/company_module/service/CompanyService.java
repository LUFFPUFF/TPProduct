package com.example.domain.api.company_module.service;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.domain.dto.CompanyDto;
import com.example.domain.dto.CompanyWithMembersDto;

public interface CompanyService {
    Company createCompany(CompanyDto companyDto);
    CompanyWithMembersDto findCompany(String email);
    void disbandCompany(String email);


}

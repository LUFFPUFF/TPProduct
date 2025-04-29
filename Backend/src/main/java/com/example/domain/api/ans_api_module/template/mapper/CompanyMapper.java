package com.example.domain.api.ans_api_module.template.mapper;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.repository.company_subscription_module.CompanyRepository;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CompanyMapper {

    @Autowired
    private CompanyRepository companyRepository;

    @Named("mapCompany")
    public Company mapCompany(Integer companyId) {
        Optional<Company> company = companyRepository.findById(companyId);
        return company
                .orElseThrow(() -> new RuntimeException("Company not found"));
    }
}

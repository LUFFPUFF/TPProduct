package com.example.domain.api.company_api_test.service;

import com.example.domain.dto.CompanyDto;
import org.springframework.stereotype.Service;

import java.util.List;

public interface CompanyService {

    CompanyDto createCompany(CompanyDto companyDto);
    CompanyDto getCompanyById(Integer id);
    List<CompanyDto> getAllCompanies();
    CompanyDto updateCompany(Integer id, CompanyDto companyDto);
    void deleteCompany(Integer id);
}
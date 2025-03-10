package com.example.domain.api.company_api_test.service.impl;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.repository.company_subscription_module.CompanyRepository;
import com.example.domain.api.company_api_test.service.CompanyService;
import com.example.domain.dto.company_module.CompanyDto;
import com.example.domain.dto.mapper.MapperDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final MapperDto mapperDto;


    @Override
    public CompanyDto createCompany(CompanyDto companyDto) {
        Company company = mapperDto.toEntityCompany(companyDto);
        company.setCreatedAt(LocalDateTime.now());
        Company savedCompany = companyRepository.save(company);
        return mapperDto.toDtoCompany(savedCompany);
    }

    @Override
    public CompanyDto getCompanyById(Integer id) {
        Optional<Company> companyOptional = companyRepository.findById(id);
        return companyOptional.map(mapperDto::toDtoCompany)
                .orElseThrow(() -> new RuntimeException("Компания с ID " + id + " не найдена"));
    }

    @Override
    public List<CompanyDto> getAllCompanies() {
        return List.of();
    }

    @Override
    public CompanyDto updateCompany(Integer id, CompanyDto companyDto) {
        return null;
    }

    @Override
    public void deleteCompany(Integer id) {

    }
}

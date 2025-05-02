package com.example.domain.api.company_module.service.impl;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.CompanyRepository;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.company_module.exception_handler_company.NotFoundCompanyException;
import com.example.domain.api.company_module.service.CompanyMembersService;
import com.example.domain.api.company_module.service.CompanyService;
import com.example.domain.dto.CompanyDto;
import com.example.domain.dto.CompanyWithMembersDto;
import com.example.domain.dto.MemberDto;
import com.example.domain.dto.mapper.MapperDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {
    private final CompanyRepository companyRepository;

    private final MapperDto mapperDto;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Company createCompany(CompanyDto companyDto) {
        Company company = mapperDto.toEntityCompany(companyDto);
        company.setCreatedAt(LocalDateTime.now());
        company.setUpdatedAt(LocalDateTime.now());
        companyRepository.save(company);
        return company;
    }

    @Override
    public CompanyWithMembersDto findCompany(String email) {
        Company company = userRepository.findByEmail(email).map(User::getCompany).orElseThrow(NotFoundCompanyException::new);
        return CompanyWithMembersDto.builder()
                .company(mapperDto.toDtoCompany(company))
                .members(findMembers(company))
                .build();
    }

    @Override
    public void disbandCompany(String email) {

    }

    @Override
    public CompanyWithMembersDto findCompanyWithId(Integer id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(NotFoundCompanyException::new);

        return CompanyWithMembersDto.builder()
                .company(mapperDto.toDtoCompany(company))
                .members(findMembers(company))
                .build();
    }

    public List<MemberDto> findMembers(Company company) {
        return userRepository.getAllByCompanyId(company.getId()).stream()
                .map(user -> MemberDto.builder()
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build()).toList();
    }

}



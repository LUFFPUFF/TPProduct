package com.example.domain.api.company_module.service;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.domain.dto.CompanyDto;
import com.example.domain.dto.CompanyWithMembersDto;
import com.example.domain.dto.MemberDto;

import java.util.List;

public interface CompanyService {

    Company createCompany(CompanyDto companyDto);
    CompanyWithMembersDto findCompany(String email);
    void disbandCompany(String email);
    List<MemberDto> findMembers(Company company);
    CompanyWithMembersDto findCompanyWithId(Integer id);


}

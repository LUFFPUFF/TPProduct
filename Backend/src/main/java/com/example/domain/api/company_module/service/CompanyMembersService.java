package com.example.domain.api.company_module.service;

import com.example.database.model.company_subscription_module.Company;
import com.example.domain.dto.CompanyWithMembersDto;
import com.example.domain.dto.MemberDto;

import java.util.List;

public interface CompanyMembersService{
    List<MemberDto> findMembers(Company company);
    CompanyWithMembersDto addMember(String memberEmail, String myEmail);
    CompanyWithMembersDto removeMember(String memberEmail, String myEmail);
    void leave(String email);
}

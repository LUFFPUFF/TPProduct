package com.example.domain.api.company_module.service;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.domain.api.company_module.dto.MemberRoleReqDto;
import com.example.domain.dto.CompanyWithMembersDto;
import com.example.domain.dto.MemberDto;

import java.util.List;

public interface CompanyMembersService{
    List<MemberDto> findMembers(Company company);
    CompanyWithMembersDto addMember(String memberEmail, String myEmail);
    CompanyWithMembersDto removeMember(String memberEmail, String myEmail);
    void addMemberRole(MemberRoleReqDto memberRoleReqDto);
    void removeMemberRole(MemberRoleReqDto memberRoleReqDto);

}

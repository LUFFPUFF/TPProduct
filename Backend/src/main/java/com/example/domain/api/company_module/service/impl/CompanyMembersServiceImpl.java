package com.example.domain.api.company_module.service.impl;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.authentication_module.service.interfaces.RoleService;
import com.example.domain.api.company_module.exception_handler_company.NotFoundCompanyException;
import com.example.domain.api.company_module.service.CompanyMembersService;
import com.example.domain.api.company_module.exception_handler_company.SelfMemberDisbandException;
import com.example.domain.api.subscription_module.service.SubscriptionService;
import com.example.domain.dto.CompanyWithMembersDto;
import com.example.domain.dto.MemberDto;
import com.example.domain.dto.mapper.MapperDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CompanyMembersServiceImpl implements CompanyMembersService {
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final SubscriptionService subscriptionService;
    private final MapperDto mapperDto;

    @Override
    public List<MemberDto> findMembers(Company company) {
        return userRepository.getUsersByCompany(company).stream().map(user -> MemberDto.builder()
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build()).toList();
    }

    @Override
    @Transactional
    public CompanyWithMembersDto addMember(String memberEmail, String myEmail) {
        Company company = userRepository.findByEmail(myEmail).map(User::getCompany).orElseThrow(NotFoundCompanyException::new);
        subscriptionService.addOperatorCount(company);
        roleService.addRole(memberEmail, Role.OPERATOR);
        userRepository.updateCompanyByEmail(memberEmail, company);
        return CompanyWithMembersDto.builder()
                .company(mapperDto.toDtoCompany(company))
                .members(findMembers(company))
                .build();
    }

    @Override
    @Transactional
    public CompanyWithMembersDto removeMember(String memberEmail, String myEmail) {
        if(Objects.equals(memberEmail, myEmail)) {
            throw new SelfMemberDisbandException();
        }
        Company company = userRepository.findByEmail(myEmail).map(User::getCompany).orElseThrow(NotFoundCompanyException::new);
        subscriptionService.subtractOperatorCount(company);
        roleService.removeRole(memberEmail, Role.OPERATOR);
        return CompanyWithMembersDto.builder()
                .company(mapperDto.toDtoCompany(company))
                .members(findMembers(company))
                .build();
    }

    @Override
    @Transactional
    public void leave(String email) {
        Company company = userRepository.findByEmail(email).map(User::getCompany).orElseThrow(NotFoundCompanyException::new);
        if(company.getContactEmail().equals(email)){
            throw new SelfMemberDisbandException();
        }
        subscriptionService.subtractOperatorCount(company);
        roleService.removeRole(email, Role.OPERATOR);
    }

}

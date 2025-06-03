package com.example.domain.api.company_module.service.impl;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.authentication_module.service.interfaces.CurrentUserDataService;
import com.example.domain.api.authentication_module.service.interfaces.RoleService;
import com.example.domain.api.company_module.dto.MemberRoleReqDto;
import com.example.domain.api.company_module.exception_handler_company.NotFoundCompanyException;
import com.example.domain.api.company_module.exception_handler_company.UserAlreadyInCompanyExeption;
import com.example.domain.api.company_module.exception_handler_company.UserNotInCompanyException;
import com.example.domain.api.company_module.service.CompanyMembersService;
import com.example.domain.api.company_module.exception_handler_company.SelfMemberDisbandException;
import com.example.domain.api.crm_module.service.DealService;
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
    private final CurrentUserDataService currentUserDataService;
    private final DealService dealService;

    @Override
    @Transactional
    public List<MemberDto> findMembers(Company company) {
        return userRepository.findByCompanyId(company.getId()).stream()
                .map(user -> MemberDto.builder()
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .roles(roleService.getUserRoles(user.getEmail()))
                        .build()).toList();
    }

    @Override
    @Transactional
    public CompanyWithMembersDto addMember(String memberEmail, String myEmail) {
        Company company = userRepository.findByEmail(myEmail).map(User::getCompany).orElseThrow(NotFoundCompanyException::new);
        subscriptionService.addOperatorCount(company);
        if(currentUserDataService.getUser(memberEmail).getCompany() != null){
            throw new UserAlreadyInCompanyExeption();
        }
        roleService.addRole(memberEmail, Role.OPERATOR);
        userRepository.updateByCompanyIdAndEmail(company.getId(), memberEmail);
        return CompanyWithMembersDto.builder()
                .company(mapperDto.toDtoCompany(company))
                .members(findMembers(company))
                .build();
    }

    @Override
    @Transactional
    public CompanyWithMembersDto removeMember(String memberEmail, String myEmail) {
        if (Objects.equals(memberEmail, myEmail)) {
            throw new SelfMemberDisbandException();
        }
        Company company = userRepository.findByEmail(myEmail).map(User::getCompany).orElseThrow(NotFoundCompanyException::new);
        subscriptionService.subtractOperatorCount(company);
        roleService.removeRole(memberEmail, Role.OPERATOR);
        roleService.removeRole(memberEmail, Role.MANAGER);
        User user = currentUserDataService.getUser(memberEmail);
        user.setCompany(null);
        userRepository.save(user);
        dealService.changeDealUser(memberEmail);
        return CompanyWithMembersDto.builder()
                .company(mapperDto.toDtoCompany(company))
                .members(findMembers(company))
                .build();
    }

    @Override
    @Transactional
    public void addMemberRole(MemberRoleReqDto memberRoleReqDto) {
             if(!currentUserDataService.getUser(memberRoleReqDto.getEmail().getEmail()).getCompany().equals(currentUserDataService.getUserCompany())){
                 throw new UserNotInCompanyException();
             }
             roleService.addRole(memberRoleReqDto.getEmail().getEmail(), memberRoleReqDto.getRole());
    }

    @Override
    public void removeMemberRole(MemberRoleReqDto memberRoleReqDto) {
        if(!currentUserDataService.getUser(memberRoleReqDto.getEmail().getEmail()).getCompany().equals(currentUserDataService.getUserCompany())){
            throw new UserNotInCompanyException();
        }
        roleService.removeRole(memberRoleReqDto.getEmail().getEmail(),memberRoleReqDto.getRole());
    }



}

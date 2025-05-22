package com.example.domain.api.company_module.controller;


import com.example.domain.api.authentication_module.service.interfaces.CurrentUserDataService;
import com.example.domain.api.company_module.service.CompanyMembersService;
import com.example.domain.api.company_module.service.CompanyService;
import com.example.domain.dto.CompanyWithMembersDto;

import com.example.domain.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("api/company")
@RequiredArgsConstructor
public class CompanyController {
private final CompanyService companyService;
private final CompanyMembersService companyMembersService;
private final CurrentUserDataService currentUserDataService;
    @GetMapping("/get")
    public ResponseEntity<CompanyWithMembersDto> getCompany() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(companyService.findCompany(email));
    }
    @GetMapping("/get/member-list")
    public ResponseEntity<List<MemberDto>> getCompanyMembers() {
        return ResponseEntity.ok(companyMembersService.findMembers(currentUserDataService.getUserCompany()));
    }

}

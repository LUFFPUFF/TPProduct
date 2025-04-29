package com.example.domain.api.company_module.controller;


import com.example.domain.api.company_module.service.CompanyMembersService;
import com.example.domain.api.company_module.service.CompanyService;
import com.example.domain.dto.CompanyWithMembersDto;
import com.example.domain.dto.LoginReqDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/company")
@RequiredArgsConstructor
public class CompanyController {
private final CompanyService companyService;
private final CompanyMembersService companyMembersService;
    @GetMapping("/get")
    public ResponseEntity<CompanyWithMembersDto> getCompany() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(companyService.findCompany(email));
    }

    @PostMapping("/add")
    public ResponseEntity<CompanyWithMembersDto> addMember(@RequestBody @Validated LoginReqDto memberEmail) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(companyMembersService.addMember(memberEmail.getEmail(), email));
    }

    @PostMapping("/join")
    public String joinMember() {
        return "member";
    }

    @PostMapping("/disband")
    public String disbandCompany() {
        return "disband";
    }

    @PostMapping
    public void leaveCompany() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        companyMembersService.leave(email);
    }

}

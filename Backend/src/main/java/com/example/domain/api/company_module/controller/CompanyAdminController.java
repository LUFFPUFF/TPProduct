package com.example.domain.api.company_module.controller;

import com.example.domain.api.company_module.dto.MemberRoleReqDto;
import com.example.domain.api.company_module.service.CompanyMembersService;
import com.example.domain.api.company_module.service.CompanyService;
import com.example.domain.dto.CompanyWithMembersDto;
import com.example.domain.dto.EmailDto;
import com.example.domain.dto.LoginReqDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/company/admin")
@RequiredArgsConstructor
public class CompanyAdminController {
    private final CompanyMembersService companyMembersService;
    @PostMapping("/add-member")
    public ResponseEntity<CompanyWithMembersDto> addMember(@RequestBody @Validated EmailDto memberEmail) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(companyMembersService.addMember(memberEmail.getEmail(), email));
    }
    @PostMapping("/member/remove")
    public ResponseEntity<CompanyWithMembersDto> removeMember(@RequestBody @Validated EmailDto memberEmail) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(companyMembersService.removeMember(memberEmail.getEmail(),email));
    }
    @PostMapping("/member/give-role")
    public ResponseEntity<HttpStatus> addMemberRole(@RequestBody @Validated MemberRoleReqDto memberRole) {
        companyMembersService.addMemberRole(memberRole);
        return ResponseEntity.ok(HttpStatus.OK);
    }
    @PostMapping("/member/remove-role")
    public ResponseEntity<HttpStatus> removeMemberRole(@RequestBody @Validated MemberRoleReqDto memberRole){
        companyMembersService.removeMemberRole(memberRole);
        return ResponseEntity.ok(HttpStatus.OK);
    }


}

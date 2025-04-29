package com.example.domain.api.company_module.controller;

import com.example.domain.api.company_module.service.CompanySettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/company/settings")
@RequiredArgsConstructor
public class CompanySettingsController {
    private final CompanySettingsService companySettingsService;
    @PostMapping("/name")
    public String  changeCompanyName(String name){
        return name;
    }

}

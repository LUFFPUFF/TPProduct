package com.example.domain.api.company_module.controller;

import com.example.domain.api.company_module.dto.ChangeCompanyDataDto;
import com.example.domain.api.company_module.service.CompanySettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("api/company/settings")
@RequiredArgsConstructor
public class CompanySettingsController {
    private final CompanySettingsService companySettingsService;
    @PostMapping("/data")
    public ResponseEntity<Void> changeCompanyData(@RequestBody ChangeCompanyDataDto changeCompanyDataDto) {
        companySettingsService.changeCompanyData(changeCompanyDataDto);
        return ResponseEntity.noContent().build();
    }
}

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
    @PostMapping("/name")
    public ResponseEntity<ChangeCompanyDataDto> changeCompanyName(@RequestBody ChangeCompanyDataDto changeCompanyDataDto){
         return ResponseEntity.ok(ChangeCompanyDataDto.builder().data(companySettingsService.changeName(changeCompanyDataDto.getData())).build());
    }
    @PostMapping("/description")
    public ResponseEntity<ChangeCompanyDataDto>  changeCompanyDescription(@RequestBody ChangeCompanyDataDto changeCompanyDataDto){
        return ResponseEntity.ok(ChangeCompanyDataDto.builder().data(companySettingsService.changeDescription(changeCompanyDataDto.getData())).build());
    }

}

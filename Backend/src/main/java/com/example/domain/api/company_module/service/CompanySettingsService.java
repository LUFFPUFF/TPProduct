package com.example.domain.api.company_module.service;

public interface CompanySettingsService {
    String changeName(String name);
    String changeDescription(String description);
    String changeOwner(String email);
}

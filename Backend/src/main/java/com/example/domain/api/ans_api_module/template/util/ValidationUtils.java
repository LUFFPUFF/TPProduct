package com.example.domain.api.ans_api_module.template.util;

import com.example.database.repository.company_subscription_module.CompanyRepository;
import com.example.domain.api.ans_api_module.template.exception.ValidationException;
import com.example.domain.api.ans_api_module.template.dto.request.PredefinedAnswerUploadDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ValidationUtils {

    private final CompanyRepository companyRepository;

    public void validateAnswerDto(PredefinedAnswerUploadDto dto) {
        if (!companyRepository.existsById(dto.getCompanyDto().getId())) {
            throw new ValidationException("Company not found: " + dto.getCompanyDto().getId());
        }

        if (dto.getAnswer().length() > 5000) {
            throw new ValidationException("Answer exceeds 5000 chars");
        }
    }
}

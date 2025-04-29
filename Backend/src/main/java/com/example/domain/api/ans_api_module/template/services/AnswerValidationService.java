package com.example.domain.api.ans_api_module.template.services;

import com.example.database.repository.company_subscription_module.CompanyRepository;
import com.example.domain.api.ans_api_module.template.exception.ValidationException;
import com.example.domain.api.ans_api_module.template.dto.request.PredefinedAnswerUploadDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class AnswerValidationService {

    private final CompanyRepository companyRepository;

    public void validateAnswerDto(@Valid PredefinedAnswerUploadDto dto) {

        validateBusinessRules(dto);

        if (!companyRepository.existsById(dto.getCompanyDto().getId())) {
            throw new ValidationException(
                    String.format("Company with id %d not found", dto.getCompanyDto().getId()));
        }
    }

    private void validateBusinessRules(PredefinedAnswerUploadDto dto) {
        if (dto.getAnswer().length() > 5000) {
            throw new ValidationException("Answer exceeds maximum length of 5000 characters");
        }

        if (dto.getTitle().length() > 255) {
            throw new ValidationException("Title exceeds maximum length of 255 characters");
        }
    }

    public void validateForUpdate(PredefinedAnswerUploadDto dto, Long existingId) {
        validateAnswerDto(dto);
        if (existingId == null) {
            throw new ValidationException("Existing answer ID must be provided for update");
        }
    }
}

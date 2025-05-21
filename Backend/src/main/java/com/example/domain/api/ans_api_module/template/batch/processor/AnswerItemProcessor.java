package com.example.domain.api.ans_api_module.template.batch.processor;

import com.example.database.model.ai_module.PredefinedAnswer;
import com.example.database.repository.company_subscription_module.CompanyRepository;
import com.example.domain.api.ans_api_module.template.mapper.PredefinedAnswerMapper;
import com.example.domain.api.ans_api_module.template.services.AnswerValidationService;
import com.example.domain.api.ans_api_module.template.dto.request.PredefinedAnswerUploadDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnswerItemProcessor implements ItemProcessor<PredefinedAnswerUploadDto, PredefinedAnswer> {

    private final CompanyRepository companyRepository;
    private final PredefinedAnswerMapper answerMapper;

    @NotNull
    @Override
    public PredefinedAnswer process(@NotNull PredefinedAnswerUploadDto dto) {
        PredefinedAnswer answer = answerMapper.toEntity(dto);
        answer.setCompany(companyRepository.findById(dto.getCompanyDto().getId())
                .orElseThrow(() -> new RuntimeException("Company not found")));
        return answer;
    }
}

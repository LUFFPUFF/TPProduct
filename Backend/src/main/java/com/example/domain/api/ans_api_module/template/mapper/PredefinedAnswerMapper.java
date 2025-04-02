package com.example.domain.api.ans_api_module.template.mapper;

import com.example.database.model.ai_module.PredefinedAnswer;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.domain.dto.ans_module.predefined_answer.request.PredefinedAnswerUploadDto;
import com.example.domain.dto.ans_module.predefined_answer.response.AnswerResponse;
import org.mapstruct.*;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = CompanyMapper.class)
public interface PredefinedAnswerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "company", source = "companyId", qualifiedByName = "mapCompany")
    @Mapping(target = "normalizedTemplateCode", source = "templateCode", qualifiedByName = "normalizeTemplateCode")
    PredefinedAnswer toEntity(PredefinedAnswerUploadDto dto);

    @Mapping(target = "companyId", source = "company.id")
    PredefinedAnswerUploadDto toDto(PredefinedAnswer entity);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "answer", source = "answer")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "templateCode", source = "normalizedTemplateCode")
    @Mapping(target = "companyName", source = "company.name")
    @Mapping(target = "createdAt", expression = "java(entity.getCreatedAt().toInstant(java.time.ZoneOffset.UTC))")
    @Mapping(target = "isActive", constant = "true")
    AnswerResponse toResponseDto(PredefinedAnswer entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateFromDto(PredefinedAnswerUploadDto dto, @MappingTarget PredefinedAnswer answer);

    @Named("mapCompany")
    default Company mapCompany(Integer companyId) {
        if (companyId == null) {
            return null;
        }
        Company company = new Company();
        company.setId(companyId);
        return company;
    }

    @Named("normalizeTemplateCode")
    default String normalizeTemplateCode(String templateCode) {
        if (templateCode == null) {
            return null;
        }
        return templateCode.trim().toUpperCase();
    }
}

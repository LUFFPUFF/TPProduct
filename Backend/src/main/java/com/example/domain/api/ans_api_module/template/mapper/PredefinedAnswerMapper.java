package com.example.domain.api.ans_api_module.template.mapper;

import com.example.database.model.ai_module.PredefinedAnswer;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.domain.api.ans_api_module.template.dto.request.PredefinedAnswerUploadDto;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = CompanyMapper.class)
public interface PredefinedAnswerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", source = "companyDto")
    PredefinedAnswer toEntity(PredefinedAnswerUploadDto dto);

    @Mapping(target = "companyDto", source = "company")
    PredefinedAnswerUploadDto toDto(PredefinedAnswer entity);

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
}

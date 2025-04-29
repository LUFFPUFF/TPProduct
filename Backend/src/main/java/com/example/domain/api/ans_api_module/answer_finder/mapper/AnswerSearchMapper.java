package com.example.domain.api.ans_api_module.answer_finder.mapper;

import com.example.database.model.ai_module.PredefinedAnswer;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.domain.api.ans_api_module.answer_finder.domain.TrustScore;
import com.example.domain.api.ans_api_module.answer_finder.domain.dto.PredefinedAnswerDto;
import com.example.domain.api.ans_api_module.answer_finder.dto.AnswerSearchResultItem;
import com.example.domain.api.ans_api_module.answer_finder.search.dto.ScoredAnswerCandidate;
import com.example.domain.dto.company_module.CompanyDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AnswerSearchMapper {

    List<AnswerSearchResultItem> toAnswerSearchResultItemList(List<ScoredAnswerCandidate> scoredCandidates);

    @Mapping(source = "combinedScore", target = "score")
    @Mapping(source = "originalAnswer", target = "answer")
    AnswerSearchResultItem toAnswerSearchResultItem(ScoredAnswerCandidate scoredCandidate);

    @Mapping(source = "trustScore", target = "trustScore", qualifiedByName = "mapTrustScoreValue")
    @Mapping(source = "company", target = "company", qualifiedByName = "mapCompanyToDto")
    PredefinedAnswerDto toPredefinedAnswerDto(PredefinedAnswer originalAnswer);

    @Named("mapTrustScoreValue")
    default double mapTrustScoreValue(TrustScore trustScoreValue) {
        return trustScoreValue != null ? trustScoreValue.getScore() : 0.0;
    }

    @Named("mapCompanyToDto")
    default CompanyDto mapCompanyToDto(Company company) {
        if (company == null) {
            return null;
        }
        return CompanyDto.builder()
                .id(company.getId())
                .name(company.getName())
                .contactEmail(company.getContactEmail())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }
}

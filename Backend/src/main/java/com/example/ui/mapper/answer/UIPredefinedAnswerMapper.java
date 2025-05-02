package com.example.ui.mapper.answer;

import com.example.domain.api.ans_api_module.template.dto.request.PredefinedAnswerUploadDto;
import com.example.domain.api.ans_api_module.template.dto.response.AnswerResponse;
import com.example.domain.dto.CompanyDto;
import com.example.ui.dto.answer.UiPredefinedAnswerDto;
import com.example.ui.dto.answer.UiPredefinedAnswerRequest;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UIPredefinedAnswerMapper {

    UiPredefinedAnswerDto toUiDto(AnswerResponse answerResponse);

    PredefinedAnswerUploadDto toServiceDto(UiPredefinedAnswerRequest request);

    List<UiPredefinedAnswerDto> toUiDtoList(List<AnswerResponse> answerResponseList);

    default CompanyDto mapCompanyIdToCompanyDto(Integer companyId) {
        if (companyId == null) {
            return null;
        }
        CompanyDto companyDto = new CompanyDto();
        companyDto.setId(companyId);
        return companyDto;
    }
}

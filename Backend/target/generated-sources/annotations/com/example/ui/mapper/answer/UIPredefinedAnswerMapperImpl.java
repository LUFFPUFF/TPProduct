package com.example.ui.mapper.answer;

import com.example.domain.api.ans_api_module.template.dto.request.PredefinedAnswerUploadDto;
import com.example.domain.api.ans_api_module.template.dto.response.AnswerResponse;
import com.example.ui.dto.answer.UiPredefinedAnswerDto;
import com.example.ui.dto.answer.UiPredefinedAnswerRequest;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-22T17:00:26+0300",
    comments = "version: 1.6.0.Beta1, compiler: javac, environment: Java 21.0.7 (Microsoft)"
)
@Component
public class UIPredefinedAnswerMapperImpl implements UIPredefinedAnswerMapper {

    @Override
    public UiPredefinedAnswerDto toUiDto(AnswerResponse answerResponse) {
        if ( answerResponse == null ) {
            return null;
        }

        UiPredefinedAnswerDto.UiPredefinedAnswerDtoBuilder uiPredefinedAnswerDto = UiPredefinedAnswerDto.builder();

        uiPredefinedAnswerDto.id( answerResponse.getId() );
        uiPredefinedAnswerDto.title( answerResponse.getTitle() );
        uiPredefinedAnswerDto.answer( answerResponse.getAnswer() );
        uiPredefinedAnswerDto.category( answerResponse.getCategory() );
        uiPredefinedAnswerDto.companyName( answerResponse.getCompanyName() );
        uiPredefinedAnswerDto.createdAt( answerResponse.getCreatedAt() );

        return uiPredefinedAnswerDto.build();
    }

    @Override
    public PredefinedAnswerUploadDto toServiceDto(UiPredefinedAnswerRequest request) {
        if ( request == null ) {
            return null;
        }

        PredefinedAnswerUploadDto.PredefinedAnswerUploadDtoBuilder predefinedAnswerUploadDto = PredefinedAnswerUploadDto.builder();

        predefinedAnswerUploadDto.category( request.getCategory() );
        predefinedAnswerUploadDto.title( request.getTitle() );
        predefinedAnswerUploadDto.answer( request.getAnswer() );

        return predefinedAnswerUploadDto.build();
    }

    @Override
    public List<UiPredefinedAnswerDto> toUiDtoList(List<AnswerResponse> answerResponseList) {
        if ( answerResponseList == null ) {
            return null;
        }

        List<UiPredefinedAnswerDto> list = new ArrayList<UiPredefinedAnswerDto>( answerResponseList.size() );
        for ( AnswerResponse answerResponse : answerResponseList ) {
            list.add( toUiDto( answerResponse ) );
        }

        return list;
    }
}

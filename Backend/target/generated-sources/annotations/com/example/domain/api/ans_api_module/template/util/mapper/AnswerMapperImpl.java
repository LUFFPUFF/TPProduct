package com.example.domain.api.ans_api_module.template.util.mapper;

import com.example.database.model.ai_module.PredefinedAnswer;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.domain.dto.ans_module.predefined_answer.files.CsvAnswerDto;
import com.example.domain.dto.ans_module.predefined_answer.files.JsonAnswerDto;
import com.example.domain.dto.ans_module.predefined_answer.request.AnswerCreateRequest;
import com.example.domain.dto.ans_module.predefined_answer.request.AnswerUpdateRequest;
import com.example.domain.dto.ans_module.predefined_answer.response.PredefinedAnswerResponse;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-03-31T20:04:21+0300",
    comments = "version: 1.5.3.Final, compiler: javac, environment: Java 22.0.1 (Oracle Corporation)"
)
@Component
public class AnswerMapperImpl implements AnswerMapper {

    @Override
    public PredefinedAnswer toEntity(PredefinedAnswerResponse d) {
        if ( d == null ) {
            return null;
        }

        PredefinedAnswer predefinedAnswer = new PredefinedAnswer();

        predefinedAnswer.setId( d.getId() );
        predefinedAnswer.setCategory( d.getCategory() );
        predefinedAnswer.setTitle( d.getTitle() );
        predefinedAnswer.setAnswer( d.getAnswer() );
        predefinedAnswer.setCreatedAt( d.getCreatedAt() );

        return predefinedAnswer;
    }

    @Override
    public List<PredefinedAnswer> toEntityList(List<PredefinedAnswerResponse> ds) {
        if ( ds == null ) {
            return null;
        }

        List<PredefinedAnswer> list = new ArrayList<PredefinedAnswer>( ds.size() );
        for ( PredefinedAnswerResponse predefinedAnswerResponse : ds ) {
            list.add( toEntity( predefinedAnswerResponse ) );
        }

        return list;
    }

    @Override
    public List<PredefinedAnswerResponse> toDtoList(List<PredefinedAnswer> es) {
        if ( es == null ) {
            return null;
        }

        List<PredefinedAnswerResponse> list = new ArrayList<PredefinedAnswerResponse>( es.size() );
        for ( PredefinedAnswer predefinedAnswer : es ) {
            list.add( toDto( predefinedAnswer ) );
        }

        return list;
    }

    @Override
    public PredefinedAnswer toEntity(AnswerCreateRequest dto) {
        if ( dto == null ) {
            return null;
        }

        PredefinedAnswer predefinedAnswer = new PredefinedAnswer();

        predefinedAnswer.setCategory( dto.getCategory() );
        predefinedAnswer.setTitle( dto.getTitle() );
        predefinedAnswer.setAnswer( dto.getAnswer() );

        predefinedAnswer.setCompany( companyRepository.findById(dto.getCompanyId()).orElseThrow(() -> new CompanyNotFoundException(dto.getCompanyId())) );

        return predefinedAnswer;
    }

    @Override
    public PredefinedAnswer toEntity(AnswerUpdateRequest dto) {
        if ( dto == null ) {
            return null;
        }

        PredefinedAnswer predefinedAnswer = new PredefinedAnswer();

        predefinedAnswer.setId( dto.getId() );
        predefinedAnswer.setCategory( dto.getCategory() );
        predefinedAnswer.setTitle( dto.getTitle() );
        predefinedAnswer.setAnswer( dto.getAnswer() );

        return predefinedAnswer;
    }

    @Override
    public PredefinedAnswerResponse toDto(PredefinedAnswer entity) {
        if ( entity == null ) {
            return null;
        }

        PredefinedAnswerResponse predefinedAnswerResponse = new PredefinedAnswerResponse();

        predefinedAnswerResponse.setCompanyId( entityCompanyId( entity ) );
        predefinedAnswerResponse.setId( entity.getId() );
        predefinedAnswerResponse.setCategory( entity.getCategory() );
        predefinedAnswerResponse.setTitle( entity.getTitle() );
        predefinedAnswerResponse.setAnswer( entity.getAnswer() );
        predefinedAnswerResponse.setCreatedAt( entity.getCreatedAt() );

        return predefinedAnswerResponse;
    }

    @Override
    public List<PredefinedAnswerResponse> toDto(List<PredefinedAnswer> entityList) {
        if ( entityList == null ) {
            return null;
        }

        List<PredefinedAnswerResponse> list = new ArrayList<PredefinedAnswerResponse>( entityList.size() );
        for ( PredefinedAnswer predefinedAnswer : entityList ) {
            list.add( toDto( predefinedAnswer ) );
        }

        return list;
    }

    @Override
    public PredefinedAnswer csvDtoToEntity(CsvAnswerDto dto, Integer companyId) {
        if ( dto == null && companyId == null ) {
            return null;
        }

        PredefinedAnswer predefinedAnswer = new PredefinedAnswer();

        if ( dto != null ) {
            predefinedAnswer.setCategory( dto.getCategory() );
            predefinedAnswer.setTitle( dto.getTitle() );
            predefinedAnswer.setAnswer( dto.getAnswer() );
        }
        predefinedAnswer.setCompany( getCompany(companyId) );

        return predefinedAnswer;
    }

    @Override
    public PredefinedAnswer jsonDtoToEntity(JsonAnswerDto dto, Integer companyId, String defaultCategory) {
        if ( dto == null && companyId == null && defaultCategory == null ) {
            return null;
        }

        PredefinedAnswer predefinedAnswer = new PredefinedAnswer();

        if ( dto != null ) {
            predefinedAnswer.setTitle( dto.getTitle() );
            predefinedAnswer.setAnswer( dto.getAnswer() );
        }
        predefinedAnswer.setCompany( getCompany(companyId) );
        predefinedAnswer.setCategory( resolveCategory(dto.getCategory(), defaultCategory) );

        return predefinedAnswer;
    }

    private Integer entityCompanyId(PredefinedAnswer predefinedAnswer) {
        if ( predefinedAnswer == null ) {
            return null;
        }
        Company company = predefinedAnswer.getCompany();
        if ( company == null ) {
            return null;
        }
        Integer id = company.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}

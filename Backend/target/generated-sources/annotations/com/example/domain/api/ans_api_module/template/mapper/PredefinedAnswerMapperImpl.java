package com.example.domain.api.ans_api_module.template.mapper;

import com.example.database.model.ai_module.PredefinedAnswer;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.domain.api.ans_api_module.template.dto.request.PredefinedAnswerUploadDto;
import com.example.domain.dto.CompanyDto;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-05T20:30:30+0300",
    comments = "version: 1.6.0.Beta1, compiler: javac, environment: Java 21.0.7 (Microsoft)"
)
@Component
public class PredefinedAnswerMapperImpl implements PredefinedAnswerMapper {

    @Override
    public PredefinedAnswer toEntity(PredefinedAnswerUploadDto dto) {
        if ( dto == null ) {
            return null;
        }

        PredefinedAnswer predefinedAnswer = new PredefinedAnswer();

        predefinedAnswer.setCompany( companyDtoToCompany( dto.getCompanyDto() ) );
        predefinedAnswer.setCategory( dto.getCategory() );
        predefinedAnswer.setTitle( dto.getTitle() );
        predefinedAnswer.setAnswer( dto.getAnswer() );

        return predefinedAnswer;
    }

    @Override
    public PredefinedAnswerUploadDto toDto(PredefinedAnswer entity) {
        if ( entity == null ) {
            return null;
        }

        PredefinedAnswerUploadDto.PredefinedAnswerUploadDtoBuilder predefinedAnswerUploadDto = PredefinedAnswerUploadDto.builder();

        predefinedAnswerUploadDto.companyDto( companyToCompanyDto( entity.getCompany() ) );
        predefinedAnswerUploadDto.category( entity.getCategory() );
        predefinedAnswerUploadDto.title( entity.getTitle() );
        predefinedAnswerUploadDto.answer( entity.getAnswer() );

        return predefinedAnswerUploadDto.build();
    }

    @Override
    public List<PredefinedAnswer> toEntityList(List<PredefinedAnswerUploadDto> dtoList) {
        if ( dtoList == null ) {
            return null;
        }

        List<PredefinedAnswer> list = new ArrayList<PredefinedAnswer>( dtoList.size() );
        for ( PredefinedAnswerUploadDto predefinedAnswerUploadDto : dtoList ) {
            list.add( toEntity( predefinedAnswerUploadDto ) );
        }

        return list;
    }

    @Override
    public void updateFromDto(PredefinedAnswerUploadDto dto, PredefinedAnswer answer) {
        if ( dto == null ) {
            return;
        }

        if ( dto.getCategory() != null ) {
            answer.setCategory( dto.getCategory() );
        }
        if ( dto.getTitle() != null ) {
            answer.setTitle( dto.getTitle() );
        }
        if ( dto.getAnswer() != null ) {
            answer.setAnswer( dto.getAnswer() );
        }
    }

    protected Company companyDtoToCompany(CompanyDto companyDto) {
        if ( companyDto == null ) {
            return null;
        }

        Company company = new Company();

        company.setId( companyDto.getId() );
        company.setName( companyDto.getName() );
        company.setContactEmail( companyDto.getContactEmail() );
        company.setCreatedAt( companyDto.getCreatedAt() );
        company.setUpdatedAt( companyDto.getUpdatedAt() );

        return company;
    }

    protected CompanyDto companyToCompanyDto(Company company) {
        if ( company == null ) {
            return null;
        }

        CompanyDto.CompanyDtoBuilder companyDto = CompanyDto.builder();

        companyDto.id( company.getId() );
        companyDto.name( company.getName() );
        companyDto.contactEmail( company.getContactEmail() );
        companyDto.createdAt( company.getCreatedAt() );
        companyDto.updatedAt( company.getUpdatedAt() );

        return companyDto.build();
    }
}

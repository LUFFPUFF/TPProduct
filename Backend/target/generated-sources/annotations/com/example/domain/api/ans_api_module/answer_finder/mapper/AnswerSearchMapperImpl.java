package com.example.domain.api.ans_api_module.answer_finder.mapper;

import com.example.database.model.ai_module.PredefinedAnswer;
import com.example.domain.api.ans_api_module.answer_finder.domain.dto.PredefinedAnswerDto;
import com.example.domain.api.ans_api_module.answer_finder.dto.AnswerSearchResultItem;
import com.example.domain.api.ans_api_module.answer_finder.search.dto.ScoredAnswerCandidate;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-18T13:36:38+0300",
    comments = "version: 1.6.0.Beta1, compiler: javac, environment: Java 22.0.2 (Amazon.com Inc.)"
)
@Component
public class AnswerSearchMapperImpl implements AnswerSearchMapper {

    @Override
    public List<AnswerSearchResultItem> toAnswerSearchResultItemList(List<ScoredAnswerCandidate> scoredCandidates) {
        if ( scoredCandidates == null ) {
            return null;
        }

        List<AnswerSearchResultItem> list = new ArrayList<AnswerSearchResultItem>( scoredCandidates.size() );
        for ( ScoredAnswerCandidate scoredAnswerCandidate : scoredCandidates ) {
            list.add( toAnswerSearchResultItem( scoredAnswerCandidate ) );
        }

        return list;
    }

    @Override
    public AnswerSearchResultItem toAnswerSearchResultItem(ScoredAnswerCandidate scoredCandidate) {
        if ( scoredCandidate == null ) {
            return null;
        }

        AnswerSearchResultItem answerSearchResultItem = new AnswerSearchResultItem();

        answerSearchResultItem.setScore( scoredCandidate.getCombinedScore() );
        answerSearchResultItem.setAnswer( toPredefinedAnswerDto( scoredCandidate.getOriginalAnswer() ) );

        return answerSearchResultItem;
    }

    @Override
    public PredefinedAnswerDto toPredefinedAnswerDto(PredefinedAnswer originalAnswer) {
        if ( originalAnswer == null ) {
            return null;
        }

        PredefinedAnswerDto predefinedAnswerDto = new PredefinedAnswerDto();

        predefinedAnswerDto.setTrustScore( mapTrustScoreValue( originalAnswer.getTrustScore() ) );
        predefinedAnswerDto.setCompany( mapCompanyToDto( originalAnswer.getCompany() ) );
        predefinedAnswerDto.setId( originalAnswer.getId() );
        predefinedAnswerDto.setTitle( originalAnswer.getTitle() );
        predefinedAnswerDto.setAnswer( originalAnswer.getAnswer() );
        predefinedAnswerDto.setCreatedAt( originalAnswer.getCreatedAt() );

        return predefinedAnswerDto;
    }
}

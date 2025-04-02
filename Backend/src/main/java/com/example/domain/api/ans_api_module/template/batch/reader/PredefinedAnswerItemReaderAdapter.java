package com.example.domain.api.ans_api_module.template.batch.reader;

import com.example.database.model.ai_module.PredefinedAnswer;
import com.example.domain.api.ans_api_module.template.mapper.PredefinedAnswerMapper;
import com.example.domain.dto.ans_module.predefined_answer.request.PredefinedAnswerUploadDto;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class PredefinedAnswerItemReaderAdapter implements ItemReader<PredefinedAnswerUploadDto> {

    private final ItemReader<PredefinedAnswer> delegate;
    private final PredefinedAnswerMapper mapper;

    public PredefinedAnswerItemReaderAdapter(ItemReader<PredefinedAnswer> delegate,
                                             PredefinedAnswerMapper mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public PredefinedAnswerUploadDto read() throws Exception {
        PredefinedAnswer entity = delegate.read();
        return mapper.toDto(entity);
    }
}

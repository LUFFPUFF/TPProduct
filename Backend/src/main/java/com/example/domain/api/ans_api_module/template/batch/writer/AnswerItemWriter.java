package com.example.domain.api.ans_api_module.template.batch.writer;

import com.example.database.model.ai_module.PredefinedAnswer;
import com.example.database.repository.ai_module.PredefinedAnswerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnswerItemWriter implements ItemWriter<PredefinedAnswer> {

    private final PredefinedAnswerRepository repository;

    @Override
    @Transactional
    public void write(Chunk<? extends PredefinedAnswer> chunk) {
        repository.saveAll(chunk.getItems());
        repository.flush();
    }
}

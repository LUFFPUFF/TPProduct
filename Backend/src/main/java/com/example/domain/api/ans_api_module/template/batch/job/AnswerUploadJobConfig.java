package com.example.domain.api.ans_api_module.template.batch.job;


import com.example.database.model.ai_module.PredefinedAnswer;
import com.example.domain.api.ans_api_module.template.batch.listener.UploadJobListener;
import com.example.domain.api.ans_api_module.template.batch.processor.AnswerItemProcessor;
import com.example.domain.api.ans_api_module.template.batch.reader.AnswerItemReader;
import com.example.domain.api.ans_api_module.template.batch.writer.AnswerItemWriter;
import com.example.domain.api.ans_api_module.template.exception.ValidationException;
import com.example.domain.dto.ans_module.predefined_answer.request.PredefinedAnswerUploadDto;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.skip.LimitCheckingItemSkipPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class AnswerUploadJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job answerUploadJob(Step answerUploadStep, UploadJobListener jobListener) {
        return new JobBuilder("answerUploadJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jobListener)
                .start(answerUploadStep)
                .build();
    }

    @Bean
    public Step answerUploadStep(
            AnswerItemReader reader,
            AnswerItemProcessor processor,
            AnswerItemWriter writer) {

        Map<Class<? extends Throwable>, Boolean> skippableExceptions = Map.of(
                ValidationException.class, true,
                DataIntegrityViolationException.class, true
        );

        return new StepBuilder("answerUploadStep", jobRepository)
                .<PredefinedAnswerUploadDto, PredefinedAnswer>chunk(500, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skipPolicy(new LimitCheckingItemSkipPolicy(100, skippableExceptions))
                .retryLimit(3)
                .retry(Exception.class)
                .listener(new AnswerItemProcessListener())
                .listener(new AnswerItemWriteListener())
                .build();
    }
}

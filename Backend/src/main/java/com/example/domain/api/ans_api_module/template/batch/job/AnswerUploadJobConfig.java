package com.example.domain.api.ans_api_module.template.batch.job;


import com.example.database.model.ai_module.PredefinedAnswer;
import com.example.domain.api.ans_api_module.template.batch.listener.UploadJobListener;
import com.example.domain.api.ans_api_module.template.batch.processor.AnswerItemProcessor;
import com.example.domain.api.ans_api_module.template.batch.reader.AnswerItemReader;
import com.example.domain.api.ans_api_module.template.batch.writer.AnswerItemWriter;
import com.example.domain.api.ans_api_module.template.exception.TextProcessingException;
import com.example.domain.api.ans_api_module.template.exception.ValidationException;
import com.example.domain.api.ans_api_module.template.dto.request.PredefinedAnswerUploadDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.skip.LimitCheckingItemSkipPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class AnswerUploadJobConfig {
//
//    private final JobRepository jobRepository;
//    private final PlatformTransactionManager transactionManager;
//
//    @Bean
//    public Job answerUploadJob(Step answerUploadStep, UploadJobListener jobListener) {
//        return new JobBuilder("answerUploadJob", jobRepository)
//                .incrementer(new RunIdIncrementer())
//                .listener(jobListener)
//                .start(answerUploadStep)
//                .build();
//    }
//
//    @Bean
//    public Step answerUploadStep(
//            AnswerItemReader reader,
//            AnswerItemProcessor processor,
//            AnswerItemWriter writer) {
//
//        return new StepBuilder("answerUploadStep", jobRepository)
//                .<PredefinedAnswerUploadDto, PredefinedAnswer>chunk(500, transactionManager)
//                .reader(reader)
//                .processor(processor)
//                .writer(writer)
//                .faultTolerant()
//                .skip(TextProcessingException.class)
//                .skip(ValidationException.class)
//                .skip(DataIntegrityViolationException.class)
//                .noSkip(FileNotFoundException.class)
//                .skipLimit(100)
//                .retry(Exception.class)
//                .retryLimit(3)
//                .build();
//    }
//
}
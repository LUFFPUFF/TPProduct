package com.example.domain.api.ans_api_module.template.config;

import com.example.database.model.ai_module.PredefinedAnswer;
import com.example.domain.dto.ans_module.predefined_answer.request.PredefinedAnswerUploadDto;
import io.micrometer.core.instrument.config.validate.ValidationException;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job answerUploadJob(Step fileProcessingStep) {
        return new JobBuilder("answerUploadJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(fileProcessingStep)
                .validator(parameters -> {
                    if (!parameters.getParameters().containsKey("companyId")) {
                        throw new JobParametersInvalidException("companyId is required");
                    }
                })
                .build();
    }

    @Bean
    public Step fileProcessingStep(
            ItemReader<PredefinedAnswerUploadDto> reader,
            ItemProcessor<PredefinedAnswerUploadDto, PredefinedAnswer> processor,
            ItemWriter<PredefinedAnswer> writer
    ) {
        return new StepBuilder("fileProcessingStep", jobRepository)
                .<PredefinedAnswerUploadDto, PredefinedAnswer>chunk(500, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skipLimit(100)
                .skip(DataIntegrityViolationException.class)
                .skip(ValidationException.class)
                .listener(new SkipListener<>() {
                    @Override
                    public void onSkipInRead(Throwable t) {
                        SkipListener.super.onSkipInRead(t);
                    }

                    @Override
                    public void onSkipInWrite(PredefinedAnswer item, Throwable t) {
                        SkipListener.super.onSkipInWrite(item, t);
                    }

                    @Override
                    public void onSkipInProcess(PredefinedAnswerUploadDto item, Throwable t) {
                        SkipListener.super.onSkipInProcess(item, t);
                    }
                })
                .build();
    }

    @Bean
    public PlatformTransactionManager batchTransactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}

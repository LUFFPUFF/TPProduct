package com.example.domain.api.ans_api_module.template.services.job.impl;

import com.example.domain.api.ans_api_module.template.mapper.JobExecutionMapper;
import com.example.domain.api.ans_api_module.template.services.job.JobStatisticsCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.*;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.dao.DataAccessException;


import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JobMetricsServiceImplTest {

    @Mock private JobStatisticsCollector statisticsCollectorMock;
    @Mock private JobExecutionMapper jobExecutionMapperMock;

    @InjectMocks
    private JobMetricsServiceImpl jobMetricsService;

    @Captor private ArgumentCaptor<JobExecution> jobExecutionCaptor;
    @Captor private ArgumentCaptor<Long> durationCaptor;

    private JobParameters mockJobParameters;
    private JobExecution mockJobExecution;
    private ExecutionContext mockExecutionContext;

    private final String JOB_NAME = "testJob";

    @BeforeEach
    void setUp() {
        reset(statisticsCollectorMock, jobExecutionMapperMock);

        mockJobParameters = new JobParameters();
        mockExecutionContext = new ExecutionContext();

        JobInstance mockJobInstance = new JobInstance(1L, JOB_NAME);
        mockJobExecution = new JobExecution(mockJobInstance, mockJobParameters); // Инициализируем базовый
    }

    // --- Тесты для recordJobStart ---
    @Test
    void recordJobStart_ShouldMapAndInitializeStatistics() {
        // Arrange
        when(jobExecutionMapperMock.createNewJobExecution(JOB_NAME, mockJobParameters))
                .thenReturn(mockJobExecution);
        doNothing().when(statisticsCollectorMock).initializeJobStatistics(any(JobExecution.class));

        // Act
        jobMetricsService.recordJobStart(JOB_NAME, mockJobParameters);

        // Assert
        verify(jobExecutionMapperMock).createNewJobExecution(JOB_NAME, mockJobParameters);
        verify(statisticsCollectorMock).initializeJobStatistics(eq(mockJobExecution)); // Проверяем, что передан правильный JobExecution
    }

    @Test
    void recordJobStart_MapperThrowsException_ShouldThrowException() {
        // Arrange
        RuntimeException mapperException = new RuntimeException("Mapper failed");
        when(jobExecutionMapperMock.createNewJobExecution(JOB_NAME, mockJobParameters))
                .thenThrow(mapperException);

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            jobMetricsService.recordJobStart(JOB_NAME, mockJobParameters);
        });
        assertEquals(mapperException, thrown);
        verifyNoInteractions(statisticsCollectorMock);
    }

    @Test
    void recordJobStart_StatisticsCollectorThrowsException_ShouldThrowException() {
        // Arrange
        when(jobExecutionMapperMock.createNewJobExecution(JOB_NAME, mockJobParameters))
                .thenReturn(mockJobExecution);
        DataAccessException collectorException = new DataAccessException("Collector init failed") {};
        doThrow(collectorException).when(statisticsCollectorMock).initializeJobStatistics(mockJobExecution);

        // Act & Assert
        DataAccessException thrown = assertThrows(DataAccessException.class, () -> {
            jobMetricsService.recordJobStart(JOB_NAME, mockJobParameters);
        });
        assertEquals(collectorException, thrown);
    }

    // --- Тесты для recordJobCompletion ---
    @Test
    void recordJobCompletion_ShouldMapAndFinalizeStatistics() {
        // Arrange
        BatchStatus status = BatchStatus.COMPLETED;
        long duration = 1000L;
        String exitCode = ExitStatus.COMPLETED.getExitCode();

        when(jobExecutionMapperMock.createCompletedJobExecution(JOB_NAME, status, duration, exitCode, mockExecutionContext))
                .thenReturn(mockJobExecution); // Маппер возвращает JobExecution
        doNothing().when(statisticsCollectorMock).finalizeJobStatistics(any(JobExecution.class), anyLong());

        // Act
        jobMetricsService.recordJobCompletion(JOB_NAME, status, duration, exitCode, mockExecutionContext);

        // Assert
        verify(jobExecutionMapperMock).createCompletedJobExecution(JOB_NAME, status, duration, exitCode, mockExecutionContext);
        verify(statisticsCollectorMock).finalizeJobStatistics(eq(mockJobExecution), eq(duration));
    }

    @Test
    void recordJobCompletion_MapperThrowsException_ShouldThrowException() {
        // Arrange
        BatchStatus status = BatchStatus.COMPLETED;
        long duration = 1000L;
        String exitCode = ExitStatus.COMPLETED.getExitCode();
        RuntimeException mapperException = new RuntimeException("Mapper failed");

        when(jobExecutionMapperMock.createCompletedJobExecution(JOB_NAME, status, duration, exitCode, mockExecutionContext))
                .thenThrow(mapperException);

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            jobMetricsService.recordJobCompletion(JOB_NAME, status, duration, exitCode, mockExecutionContext);
        });
        assertEquals(mapperException, thrown);
        verifyNoInteractions(statisticsCollectorMock);
    }

    // --- Тесты для recordJobFailure ---
    @Test
    void recordJobFailure_ShouldMapAndFinalizeStatisticsWithZeroDuration() {
        // Arrange
        BatchStatus status = BatchStatus.FAILED;
        List<Throwable> exceptions = Collections.singletonList(new RuntimeException("Job failed error"));

        when(jobExecutionMapperMock.createFailedJobExecution(JOB_NAME, status, exceptions))
                .thenReturn(mockJobExecution);
        doNothing().when(statisticsCollectorMock).finalizeJobStatistics(any(JobExecution.class), anyLong());

        // Act
        jobMetricsService.recordJobFailure(JOB_NAME, status, exceptions);

        // Assert
        verify(jobExecutionMapperMock).createFailedJobExecution(JOB_NAME, status, exceptions);
        verify(statisticsCollectorMock).finalizeJobStatistics(eq(mockJobExecution), eq(0L)); // Длительность 0 для ошибки
    }

    @Test
    void recordJobFailure_MapperThrowsException_ShouldThrowException() {
        // Arrange
        BatchStatus status = BatchStatus.FAILED;
        List<Throwable> exceptions = Collections.singletonList(new RuntimeException("Job failed error"));
        RuntimeException mapperException = new RuntimeException("Mapper failed");

        when(jobExecutionMapperMock.createFailedJobExecution(JOB_NAME, status, exceptions))
                .thenThrow(mapperException);

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            jobMetricsService.recordJobFailure(JOB_NAME, status, exceptions);
        });
        assertEquals(mapperException, thrown);
        verifyNoInteractions(statisticsCollectorMock);
    }
}
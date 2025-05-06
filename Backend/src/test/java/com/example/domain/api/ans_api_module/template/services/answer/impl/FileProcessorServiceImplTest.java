package com.example.domain.api.ans_api_module.template.services.answer.impl;

import com.example.database.repository.ai_module.PredefinedAnswerRepository;
import com.example.domain.api.ans_api_module.template.exception.InvalidFileFormatException;
import com.example.domain.api.ans_api_module.template.exception.UnsupportedFileTypeException;
import com.example.domain.api.ans_api_module.template.util.FileType;
import com.example.domain.api.ans_api_module.template.util.FileTypeDetector;
import com.example.domain.api.ans_api_module.template.dto.request.UploadFileRequest;
import com.example.domain.api.ans_api_module.template.dto.response.UploadResultResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileProcessorServiceImplTest {

    @Mock
    private JobLauncher jobLauncher;
    @Mock
    private Job answerUploadJob;
    @Mock
    private FileTypeDetector fileTypeDetector;
    @Mock
    private PredefinedAnswerRepository answerRepository;

    @InjectMocks
    private FileProcessorServiceImpl fileProcessorService;

    @Mock
    private MultipartFile mockFile;
    @Mock
    private JobExecution jobExecution;
    @Mock
    private ExecutionContext executionContext;

    private static final Integer COMPANY_ID = 123;
    private static final String CATEGORY = "testCategory";
    private static final String FILE_NAME = "test.csv";
    private static final String ORIGINAL_FILE_NAME = "test.csv";

    @BeforeEach
    void setUp() {
        when(mockFile.getName()).thenReturn(FILE_NAME);
        when(mockFile.getOriginalFilename()).thenReturn(ORIGINAL_FILE_NAME);
        reset(jobLauncher, answerUploadJob, fileTypeDetector, answerRepository, jobExecution, executionContext);
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
        when(jobExecution.getAllFailureExceptions()).thenReturn(Collections.emptyList());
    }

    // --- Успешные сценарии (без изменений, должны проходить) ---
    @Test
    void processFileUpload_Success_OverwriteTrue() throws Exception {
        UploadFileRequest request = new UploadFileRequest(mockFile, COMPANY_ID, CATEGORY, true);
        Map<String, Object> processingStats = new HashMap<>();
        processingStats.put("processedCount", 10);
        processingStats.put("duplicatesCount", 2);
        processingStats.put("rowErrors", Map.of(5, "Invalid data"));

        when(mockFile.getOriginalFilename()).thenReturn("data_to_overwrite.csv");
        when(fileTypeDetector.detect(mockFile)).thenReturn(FileType.CSV);
        when(answerRepository.deleteByCompanyIdAndCategory(COMPANY_ID, CATEGORY)).thenReturn(5);
        when(jobLauncher.run(eq(answerUploadJob), any(JobParameters.class))).thenReturn(jobExecution);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(executionContext.get(eq("processingStats"), eq(Map.class))).thenReturn(processingStats);

        UploadResultResponse response = fileProcessorService.processFileUpload(request);

        assertNotNull(response);
        assertEquals("COMPLETED", response.getStatus());
        assertEquals(10, response.getProcessedCount());
        assertEquals(2, response.getDuplicatesCount());
        assertTrue(response.getGlobalErrors().isEmpty());
        assertEquals(1, response.getRowErrors().size());
        assertEquals("Invalid data", response.getRowErrors().get(5));
        verify(fileTypeDetector, times(2)).detect(mockFile);
        verify(answerRepository, times(1)).deleteByCompanyIdAndCategory(eq(COMPANY_ID), eq(CATEGORY));
        verify(jobLauncher, times(1)).run(eq(answerUploadJob), any(JobParameters.class));
        verify(executionContext, times(1)).get(eq("processingStats"), eq(Map.class));
        verify(jobExecution, times(1)).getAllFailureExceptions();
    }

    @Test
    void processFileUpload_Success_OverwriteFalse() throws Exception {
        UploadFileRequest request = new UploadFileRequest(mockFile, COMPANY_ID, CATEGORY, false);
        Map<String, Object> processingStats = new HashMap<>();
        processingStats.put("processedCount", 15);
        processingStats.put("duplicatesCount", 0);
        processingStats.put("rowErrors", Collections.emptyMap());

        when(mockFile.getOriginalFilename()).thenReturn("data.xml"); // Используем XML
        when(fileTypeDetector.detect(mockFile)).thenReturn(FileType.XML);
        when(jobLauncher.run(eq(answerUploadJob), any(JobParameters.class))).thenReturn(jobExecution);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(executionContext.get(eq("processingStats"), eq(Map.class))).thenReturn(processingStats);

        UploadResultResponse response = fileProcessorService.processFileUpload(request);

        assertNotNull(response);
        assertEquals("COMPLETED", response.getStatus());
        assertEquals(15, response.getProcessedCount());
        assertEquals(0, response.getDuplicatesCount());
        assertTrue(response.getGlobalErrors().isEmpty());
        assertTrue(response.getRowErrors().isEmpty());
        verify(fileTypeDetector, times(2)).detect(mockFile);
        verify(answerRepository, never()).deleteByCompanyIdAndCategory(any(Integer.class), anyString());
        verify(jobLauncher, times(1)).run(eq(answerUploadJob), any(JobParameters.class));
        verify(executionContext, times(1)).get(eq("processingStats"), eq(Map.class));
    }

    // --- Сценарии с ошибками ---

    @Test
    void processFileUpload_FileTypeDetectionFails_Unsupported() throws Exception {
        // --- Arrange ---
        UploadFileRequest request = new UploadFileRequest(mockFile, COMPANY_ID, CATEGORY, false);
        String errorMessage = "Unsupported format: test.zip";
        when(mockFile.getOriginalFilename()).thenReturn("test.zip");

        // Используем doThrow() для первого вызова fileTypeDetector.detect()
        // Это может помочь, если when().thenThrow() вызывает странное поведение
        UnsupportedFileTypeException exceptionToThrow = new UnsupportedFileTypeException(errorMessage);
        doThrow(exceptionToThrow).when(fileTypeDetector).detect(mockFile);

        // --- Act ---
        // Вызываем метод и ожидаем, что он ВЕРНЕТ результат, а не выбросит исключение наружу
        UploadResultResponse response = fileProcessorService.processFileUpload(request);

        // --- Assert ---
        // Проверяем, что сервис поймал исключение и вернул FAILED статус
        assertNotNull(response, "Response should not be null, exception should have been caught by the service");
        assertEquals("FAILED", response.getStatus());
        assertEquals(0, response.getProcessedCount());
        assertEquals(0, response.getDuplicatesCount());
        assertFalse(response.getGlobalErrors().isEmpty());
        // Ожидаемое сообщение после перехвата InvalidFileFormatException(message="Unsupported file type", cause=UnsupportedFileTypeException)
        // и вызова buildErrorResponse(e)
        assertTrue(response.getGlobalErrors().get(0).contains("File processing failed: Unsupported file type"),
                "Expected error message not found in: " + response.getGlobalErrors());
        assertTrue(response.getRowErrors().isEmpty());

        // --- Verify Interactions ---
        // Ожидаем ОДИН вызов detect, так как исключение на первом вызове
        verify(fileTypeDetector, times(1)).detect(mockFile);
        verify(answerRepository, never()).deleteByCompanyIdAndCategory(any(Integer.class), anyString());
        verify(jobLauncher, never()).run(any(), any());
    }


    @Test
    void processFileUpload_JobLaunchFails() throws Exception {
        // --- Arrange ---
        UploadFileRequest request = new UploadFileRequest(mockFile, COMPANY_ID, CATEGORY, false);
        String jobLaunchErrorMessage = "Job launch failed miserably!";
        when(mockFile.getOriginalFilename()).thenReturn("good_file.csv");
        when(fileTypeDetector.detect(mockFile)).thenReturn(FileType.CSV); // Успешный detect

        // Мокируем ошибку запуска Job
        RuntimeException exceptionToThrow = new RuntimeException(jobLaunchErrorMessage);
        when(jobLauncher.run(eq(answerUploadJob), any(JobParameters.class)))
                .thenThrow(exceptionToThrow);

        // --- Act ---
        UploadResultResponse response = fileProcessorService.processFileUpload(request);

        // --- Assert ---
        assertNotNull(response);
        assertEquals("FAILED", response.getStatus());
        assertFalse(response.getGlobalErrors().isEmpty());
        // Сообщение ошибки должно содержать исходное сообщение от jobLauncher
        assertTrue(response.getGlobalErrors().get(0).contains("File processing failed: " + jobLaunchErrorMessage),
                "Expected error message not found in: " + response.getGlobalErrors());
        assertEquals(0, response.getProcessedCount());
        assertEquals(0, response.getDuplicatesCount());
        assertTrue(response.getRowErrors().isEmpty());

        // --- Verify Interactions ---
        // Ожидаем ДВА вызова detect, так как он выполнился успешно перед падением jobLauncher
        verify(fileTypeDetector, times(2)).detect(mockFile);
        verify(answerRepository, never()).deleteByCompanyIdAndCategory(any(Integer.class), anyString());
        verify(jobLauncher, times(1)).run(eq(answerUploadJob), any(JobParameters.class));
    }

    @Test
        // Переименовываем обратно, т.к. NPE не является причиной FAILED статуса в ответе
    void processFileUpload_JobExecutionFails_AndStatsMapIsNull() throws Exception {
        // --- Arrange ---
        UploadFileRequest request = new UploadFileRequest(mockFile, COMPANY_ID, CATEGORY, true);
        String jobFailedMessage = "Job processing error occurred!"; // Это сообщение ОЖИДАЕТСЯ в ответе
        Map<String, Object> emptyStats = null; // Статистика null

        when(mockFile.getOriginalFilename()).thenReturn("job_fails_null_stats.csv");
        when(fileTypeDetector.detect(mockFile)).thenReturn(FileType.CSV);
        when(answerRepository.deleteByCompanyIdAndCategory(COMPANY_ID, CATEGORY)).thenReturn(1);
        when(jobLauncher.run(eq(answerUploadJob), any(JobParameters.class))).thenReturn(jobExecution);

        // Мокируем JobExecution: статус FAILED и конкретное сообщение об ошибке
        when(jobExecution.getStatus()).thenReturn(BatchStatus.FAILED);
        when(jobExecution.getAllFailureExceptions())
                .thenReturn(List.of(new RuntimeException(jobFailedMessage)));

        // Мокируем get для статистики, чтобы он вернул null (вызовет NPE внутри getSafe*, но они будут пойманы там же)
        when(executionContext.get(eq("processingStats"), eq(Map.class))).thenReturn(emptyStats);

        // --- Act ---
        // Ожидаем нормальный возврат UploadResultResponse, отражающий состояние JobExecution
        UploadResultResponse response = fileProcessorService.processFileUpload(request);

        // --- Assert ---
        // Проверяем, что статус FAILED и ошибка взяты из JobExecution, а счетчики равны 0 из-за getSafe*
        assertNotNull(response, "Response should not be null");
        assertEquals("FAILED", response.getStatus(), "Status should be FAILED from JobExecution");
        assertEquals(0, response.getProcessedCount(), "Processed count should be 0 due to internal catch in getSafeInteger");
        assertEquals(0, response.getDuplicatesCount(), "Duplicates count should be 0 due to internal catch in getSafeInteger");
        assertFalse(response.getGlobalErrors().isEmpty(), "Global errors should not be empty");

        // ИСПРАВЛЕНО: Ожидаем сообщение об ошибке, которое было установлено для JobExecution
        assertEquals(jobFailedMessage, response.getGlobalErrors().get(0), "Global error message should be from JobExecution");
        assertTrue(response.getRowErrors().isEmpty(), "Row errors should be empty due to internal catch in getSafeMap");

        // --- Verify Interactions ---
        verify(fileTypeDetector, times(2)).detect(mockFile);
        verify(answerRepository, times(1)).deleteByCompanyIdAndCategory(eq(COMPANY_ID), eq(CATEGORY));
        verify(jobLauncher, times(1)).run(eq(answerUploadJob), any(JobParameters.class));
        verify(executionContext, times(1)).get(eq("processingStats"), eq(Map.class)); // Попытка получить статистику была
        // Ошибки JobExecution ДОЛЖНЫ были быть прочитаны методом extractGlobalErrors
        verify(jobExecution, times(1)).getAllFailureExceptions();
    }

    // Тест buildUploadResult_ThrowsNPE_WhenStatsMapIsNull УДАЛЕН


    @Test
    void buildUploadResult_HandlesNonIntegerValuesInStatsMap() {
        Map<String, Object> statsWithIssues = new HashMap<>();
        statsWithIssues.put("processedCount", "not a number");
        statsWithIssues.put("duplicatesCount", null);
        statsWithIssues.put("rowErrors", "not a map");

        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
        when(jobExecution.getAllFailureExceptions()).thenReturn(Collections.emptyList());
        when(executionContext.get(eq("processingStats"), eq(Map.class))).thenReturn(statsWithIssues);

        UploadResultResponse response = fileProcessorService.buildUploadResult(jobExecution);

        assertEquals(0, response.getProcessedCount());
        assertEquals(0, response.getDuplicatesCount());
        assertTrue(response.getRowErrors().isEmpty());
        verify(executionContext, times(1)).get(eq("processingStats"), eq(Map.class));
    }

    // --- Тесты для detectFileType и validateFile (без изменений, должны проходить) ---
    @Test
    void detectFileType_Success() throws Exception {
        when(mockFile.getOriginalFilename()).thenReturn("data.json");
        when(fileTypeDetector.detect(mockFile)).thenReturn(FileType.JSON);
        FileType detectedType = fileProcessorService.detectFileType(mockFile);
        assertEquals(FileType.JSON, detectedType);
        verify(fileTypeDetector, times(2)).detect(mockFile); // Ожидаем 2 вызова
    }

    @Test
    void detectFileType_DetectorThrowsException_ServiceWrapsIt() throws Exception {
        String detectorErrorMessage = "Detection failed";
        when(mockFile.getOriginalFilename()).thenReturn("unknown.ext");
        RuntimeException exceptionToThrow = new RuntimeException(detectorErrorMessage);
        // Мокируем ПЕРВЫЙ вызов
        doThrow(exceptionToThrow).when(fileTypeDetector).detect(mockFile);

        InvalidFileFormatException thrown = assertThrows(InvalidFileFormatException.class, () -> {
            fileProcessorService.detectFileType(mockFile);
        });

        assertEquals("Unsupported file type", thrown.getMessage());
        assertNotNull(thrown.getCause());
        assertEquals(detectorErrorMessage, thrown.getCause().getMessage());
        verify(fileTypeDetector, times(1)).detect(mockFile); // Ожидаем 1 вызов
    }

    @Test
    void validateFile_DoesNotThrowException() {
        assertDoesNotThrow(() -> {
            fileProcessorService.validateFile(mockFile);
        });
    }
}
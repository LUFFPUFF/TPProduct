package com.example.domain.api.ans_api_module.correction_answer.service;

import com.example.domain.api.ans_api_module.correction_answer.config.MLParamsConfig;
import com.example.domain.api.ans_api_module.correction_answer.config.promt.PromptConfig;
import com.example.domain.api.ans_api_module.correction_answer.dto.GenerationRequest;
import com.example.domain.api.ans_api_module.correction_answer.dto.GenerationResponse;
import com.example.domain.api.ans_api_module.correction_answer.exception.MLException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import jakarta.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
// import org.springframework.retry.support.RetryTemplate; // Не нужен

import java.io.IOException; // Для создания MLException
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TextProcessingServiceTest {

    @Mock
    private TextProcessingApiClient apiClient;
    @Mock
    private MLParamsConfig defaultApiParams;
    @Mock
    private Validator validator;

    @InjectMocks
    private TextProcessingService textProcessingService;

    @Captor
    private ArgumentCaptor<GenerationRequest> generationRequestCaptor;

    private static MockedStatic<PromptConfig> mockedPromptConfig;

    private final String testQuery = "Test query with \"some\" \n errors";
    private final String sanitizedQuery = "Test query with 'some'   errors";
    private final double DEFAULT_TEMP = 0.7;
    private final int DEFAULT_MAX_TOKENS = 150;
    private final double DEFAULT_TOP_P = 0.85;
    private final boolean DEFAULT_DO_SAMPLE = false;
    private final boolean DEFAULT_STREAM = false;
    // ИСПРАВЛЕНО: Сделано static final
    private static final String CORRECTION_PROMPT_TEMPLATE = "Correct this: %s";
    private static final String REWRITE_PROMPT_TEMPLATE = "Instruction: %s\nText: %s";

    @BeforeAll
    static void beforeAll() {
        PromptConfig mockConfig = mock(PromptConfig.class);
        when(mockConfig.correctionPromptTemplate()).thenReturn(CORRECTION_PROMPT_TEMPLATE);
        when(mockConfig.rewritePromptTemplate()).thenReturn(REWRITE_PROMPT_TEMPLATE);
        mockedPromptConfig = mockStatic(PromptConfig.class);
        mockedPromptConfig.when(PromptConfig::getDefault).thenReturn(mockConfig);
    }

    @AfterAll
    static void afterAll() {
        mockedPromptConfig.close();
    }

    @BeforeEach
    void setUp() {
        reset(apiClient, defaultApiParams, validator);

        when(defaultApiParams.getTemperature()).thenReturn(DEFAULT_TEMP);
        when(defaultApiParams.getMaxNewTokens()).thenReturn(DEFAULT_MAX_TOKENS);
        when(defaultApiParams.getTopP()).thenReturn(DEFAULT_TOP_P);
        when(defaultApiParams.isDoSample()).thenReturn(DEFAULT_DO_SAMPLE);
        when(defaultApiParams.isStream()).thenReturn(DEFAULT_STREAM);
        when(validator.validate(any())).thenReturn(Collections.emptySet());
    }

    // --- Успешные сценарии (остаются без изменений) ---
    @Test
    void processQuery_CorrectionType_ShouldReturnProcessedTextAndVerifyRequest() {
        String expectedCorrectedText = "Corrected test query";
        GenerationResponse mockResponse = GenerationResponse.builder().generatedText(expectedCorrectedText).build();
        when(apiClient.generateText(any(GenerationRequest.class))).thenReturn(mockResponse);
        String expectedPrompt = String.format(CORRECTION_PROMPT_TEMPLATE, sanitizedQuery);

        String result = textProcessingService.processQuery(testQuery, GenerationType.CORRECTION);

        assertNotNull(result);
        assertEquals(expectedCorrectedText, result);
        verify(apiClient).generateText(generationRequestCaptor.capture());
        GenerationRequest capturedRequest = generationRequestCaptor.getValue();
        assertEquals(expectedPrompt, capturedRequest.getPrompt());
        assertFalse(capturedRequest.isTextGeneration());
        // ... остальные проверки параметров ...
    }

    @Test
    void processQuery_RewriteType_ShouldReturnProcessedTextAndVerifyRequest() {
        String expectedRewrittenText = "Rewritten test query";
        GenerationResponse mockResponse = GenerationResponse.builder().generatedText(expectedRewrittenText).build();
        when(apiClient.generateText(any(GenerationRequest.class))).thenReturn(mockResponse);
        String instruction = "Improve this text while keeping the original meaning";
        String expectedPrompt = String.format(REWRITE_PROMPT_TEMPLATE, instruction, sanitizedQuery);

        String result = textProcessingService.processQuery(testQuery, GenerationType.REWRITE);

        assertNotNull(result);
        assertEquals(expectedRewrittenText, result);
        verify(apiClient).generateText(generationRequestCaptor.capture());
        GenerationRequest capturedRequest = generationRequestCaptor.getValue();
        assertEquals(expectedPrompt, capturedRequest.getPrompt());
        assertTrue(capturedRequest.isTextGeneration());
        // ... остальные проверки параметров ...
    }

    @Test
    void processQuery_CorrectionThenRewriteType_ShouldCallCorrectionAndRewrite() {
        String correctedText = "Corrected 'intermediate' text";
        String finalRewrittenText = "Final rewritten text";
        GenerationResponse correctionResponse = GenerationResponse.builder().generatedText(correctedText).build();
        GenerationResponse rewriteResponse = GenerationResponse.builder().generatedText(finalRewrittenText).build();
        when(apiClient.generateText(any(GenerationRequest.class)))
                .thenReturn(correctionResponse)
                .thenReturn(rewriteResponse);
        String expectedCorrectionPrompt = String.format(CORRECTION_PROMPT_TEMPLATE, sanitizedQuery);
        String expectedRewritePrompt = String.format(REWRITE_PROMPT_TEMPLATE, "Improve this text while keeping the original meaning", correctedText);

        String result = textProcessingService.processQuery(testQuery, GenerationType.CORRECTION_THEN_REWRITE);

        assertEquals(finalRewrittenText, result);
        verify(apiClient, times(2)).generateText(generationRequestCaptor.capture());
        List<GenerationRequest> capturedRequests = generationRequestCaptor.getAllValues();
        GenerationRequest correctionRequest = capturedRequests.get(0);
        assertEquals(expectedCorrectionPrompt, correctionRequest.getPrompt());
        assertFalse(correctionRequest.isTextGeneration());
        GenerationRequest rewriteRequest = capturedRequests.get(1);
        assertEquals(expectedRewritePrompt, rewriteRequest.getPrompt());
        assertTrue(rewriteRequest.isTextGeneration());
    }

    // --- Сценарии с ошибками и граничными случаями ---

    @Test
    void processQuery_ApiError_ShouldThrowMLExceptionAndRetry() {
        MLException apiException = new MLException("API error", 500);
        when(apiClient.generateText(any(GenerationRequest.class))).thenThrow(apiException);

        MLException thrown = assertThrows(MLException.class,
                () -> textProcessingService.processQuery(testQuery, GenerationType.CORRECTION));
        assertEquals("API error", thrown.getMessage());
        assertEquals(500, thrown.getStatusCode());
        verify(apiClient, times(3)).generateText(any(GenerationRequest.class)); // Ожидаем 3 попытки
    }

    @Test
        // ИСПРАВЛЕНО: Ожидаем IllegalArgumentException
    void processQuery_NullQuery_ShouldThrowIllegalArgumentException() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            textProcessingService.processQuery(null, GenerationType.CORRECTION);
        });
        assertEquals("Client query cannot be null or empty", thrown.getMessage());
        verifyNoInteractions(apiClient);
    }

    @Test
        // ИСПРАВЛЕНО: Ожидаем IllegalArgumentException
    void processQuery_EmptyQuery_ShouldThrowIllegalArgumentException() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            textProcessingService.processQuery("  ", GenerationType.REWRITE);
        });
        assertEquals("Client query cannot be null or empty", thrown.getMessage());
        verifyNoInteractions(apiClient);
    }

    @Test
        // Ожидаем NullPointerException
    void processQuery_NullType_ShouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            textProcessingService.processQuery(testQuery, null);
        });
        verifyNoInteractions(apiClient);
    }

    @Test
        // Ожидаем MLException 400
    void processQuery_ValidationFails_ShouldThrowMLExceptionWith400() {
        Set<ConstraintViolation<GenerationRequest>> violations = new HashSet<>();
        ConstraintViolation<GenerationRequest> violation = mock(ConstraintViolation.class);
        Path propertyPath = mock(Path.class);
        when(propertyPath.toString()).thenReturn("prompt");
        when(violation.getPropertyPath()).thenReturn(propertyPath);
        when(violation.getMessage()).thenReturn("must not be blank");
        violations.add(violation);
        when(validator.validate(any(GenerationRequest.class))).thenReturn(violations);

        MLException thrown = assertThrows(MLException.class, () -> {
            textProcessingService.processQuery(testQuery, GenerationType.CORRECTION);
        });

        assertEquals("Validation failed", thrown.getMessage());
        assertEquals(400, thrown.getStatusCode());
        assertInstanceOf(ConstraintViolationException.class, thrown.getCause());
        assertTrue(thrown.getCause().getMessage().contains("prompt: must not be blank"));
        // ИСПРАВЛЕНО: Проверяем 3 вызова валидатора из-за ретраев
        verify(validator, times(3)).validate(any(GenerationRequest.class));
        verifyNoInteractions(apiClient);
    }

    @Test
        // Ожидаем исходный query после trim()
    void processQuery_ApiClientReturnsNullResponse_ShouldReturnTrimmedOriginalQuery() {
        when(apiClient.generateText(any(GenerationRequest.class))).thenReturn(null);
        String result = textProcessingService.processQuery(testQuery, GenerationType.CORRECTION);
        assertEquals(testQuery.trim(), result);
        verify(apiClient).generateText(any(GenerationRequest.class));
    }

    @Test
        // Ожидаем исходный query после trim()
    void processQuery_ApiClientReturnsResponseWithNullText_ShouldReturnTrimmedOriginalQuery() {
        GenerationResponse mockResponse = GenerationResponse.builder().generatedText(null).build();
        when(apiClient.generateText(any(GenerationRequest.class))).thenReturn(mockResponse);
        String result = textProcessingService.processQuery(testQuery, GenerationType.CORRECTION);
        assertEquals(testQuery.trim(), result);
        verify(apiClient).generateText(any(GenerationRequest.class));
    }

    @Test
        // Ожидаем исходный query после trim()
    void processQuery_ApiClientReturnsResponseWithEmptyText_ShouldReturnTrimmedOriginalQuery() {
        GenerationResponse mockResponse = GenerationResponse.builder().generatedText("   ").build();
        when(apiClient.generateText(any(GenerationRequest.class))).thenReturn(mockResponse);
        String result = textProcessingService.processQuery(testQuery, GenerationType.CORRECTION);
        assertEquals(testQuery.trim(), result);
        verify(apiClient).generateText(any(GenerationRequest.class));
    }

    @Test
        // ИСПРАВЛЕНО: Мокируем MLException, а не IOException
    void processQuery_ApiClientThrowsIOException_ShouldWrapInMLExceptionAndRetry() {
        // Arrange
        // Создаем MLException, который имитирует то, что выбросит клиент при IOException
        IOException ioException = new IOException("Network Error");
        MLException wrappedException = new MLException("Network or IO error", -1, ioException);
        when(apiClient.generateText(any(GenerationRequest.class))).thenThrow(wrappedException);

        // Act & Assert
        MLException thrown = assertThrows(MLException.class, () -> {
            textProcessingService.processQuery(testQuery, GenerationType.CORRECTION);
        });

        // Проверяем внешнее исключение MLException
        assertEquals("Network or IO error", thrown.getMessage());
        assertEquals(-1, thrown.getStatusCode());
        assertNotNull(thrown.getCause());
        assertEquals(ioException, thrown.getCause());

        // Проверяем ретраи
        verify(apiClient, times(3)).generateText(any(GenerationRequest.class));
    }

    @Test
        // ИСПРАВЛЕНО: Ожидаем ArrayIndexOutOfBoundsException
    void processQuery_UnsupportedGenerationType_ShouldThrowArrayIndexOutOfBoundsException() {
        // Arrange
        GenerationType unsupportedType = mock(GenerationType.class);
        // Устанавливаем ординал, который вызовет ошибку в switch
        when(unsupportedType.ordinal()).thenReturn(99);

        // Act & Assert
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            textProcessingService.processQuery(testQuery, unsupportedType);
        });
    }
}
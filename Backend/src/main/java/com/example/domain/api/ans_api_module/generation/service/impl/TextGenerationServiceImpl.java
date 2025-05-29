package com.example.domain.api.ans_api_module.generation.service.impl;

import com.example.domain.api.ans_api_module.generation.config.MLParamsConfig;
import com.example.domain.api.ans_api_module.generation.exception.MLException;
import com.example.domain.api.ans_api_module.generation.model.GenerationRequest;
import com.example.domain.api.ans_api_module.generation.model.GenerationResponse;
import com.example.domain.api.ans_api_module.generation.model.enums.GenerationType;
import com.example.domain.api.ans_api_module.generation.service.IResilientTextProcessingApiClient;
import com.example.domain.api.ans_api_module.generation.service.ITextGenerationService;
import com.example.domain.api.ans_api_module.generation.util.PromptBuilderService;
import com.example.domain.api.chat_service_api.util.MdcUtil;
import com.example.domain.api.statistics_module.metrics.service.ITextProcessingMetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TextGenerationServiceImpl implements ITextGenerationService {

    private final IResilientTextProcessingApiClient resilientApiClient;
    private final MLParamsConfig apiParams;
    private final ITextProcessingMetricsService metricsService;
    private final MeterRegistry meterRegistry;

    private static final String MDC_KEY_OPERATION = "operation";
    private static final String MDC_KEY_GENERATION_TYPE_PARAM = "generationTypeParam";
    private static final String MDC_KEY_TASK_TYPE_INTERNAL = "taskTypeInternal";
    private static final String MDC_KEY_QUERY_PREVIEW = "queryPreview";
    private static final String MDC_KEY_CLIENT_MESSAGES_PREVIEW = "clientMessagesPreview";

    private static final String METRIC_PREFIX = "text_processing_";
    private static final String TIMER_PROCESS_QUERY = METRIC_PREFIX + "process_query_duration_seconds";
    private static final String TIMER_GENERATE_GENERAL_ANSWER = METRIC_PREFIX + "generate_general_answer_duration_seconds";
    private static final String COUNTER_REQUESTS_TOTAL = METRIC_PREFIX + "requests_total";
    private static final String COUNTER_ERRORS_TOTAL = METRIC_PREFIX + "errors_total";
    private static final String TIMER_ANALYZE_CLIENT_STYLE = METRIC_PREFIX + "analyze_client_style_duration_seconds";

    private static final String TAG_METHOD_NAME = "method_name";
    private static final String TAG_OUTCOME = "outcome";
    private static final String TAG_EXCEPTION_TYPE = "exception_type";
    private static final String TAG_GENERATION_TYPE = "generation_type";

    @Override
    public String processQuery(String query, GenerationType generationType, String clientPreviousMessages) throws MLException {
        return MdcUtil.withContext(
                () -> {
                    Timer.Sample sample = Timer.start(meterRegistry);
                    String operationNameForMetrics = "processQuery:" + generationType.name();
                    incrementRequestCounter(operationNameForMetrics);

                    try {
                        validateInput(query);

                        String result;
                        if (generationType == GenerationType.CORRECTION_THEN_REWRITE) {
                            String correctedText = MdcUtil.withContext(
                                    () -> processCorrectionInternal(query),
                                    MDC_KEY_OPERATION, "processQuery.correctionStep",
                                    MDC_KEY_TASK_TYPE_INTERNAL, MLParamsConfig.GenerationTaskType.CORRECTION.name()
                            );
                            result = MdcUtil.withContext(
                                    () -> processRewriteInternal(correctedText, clientPreviousMessages),
                                    MDC_KEY_OPERATION, "processQuery.rewriteStep",
                                    MDC_KEY_TASK_TYPE_INTERNAL, MLParamsConfig.GenerationTaskType.REWRITE.name()
                            );
                        } else {
                            MLParamsConfig.GenerationTaskType taskType = mapToInternalTaskType(generationType);
                            result = MdcUtil.withContext(
                                    () -> {
                                        if (taskType == MLParamsConfig.GenerationTaskType.CORRECTION) {
                                            return processCorrectionInternal(query);
                                        } else if (taskType == MLParamsConfig.GenerationTaskType.REWRITE) {
                                            return processRewriteInternal(query, clientPreviousMessages);
                                        } else {
                                            log.error("Unexpected internal task type mapping for GenerationType: {}", generationType);
                                            throw new IllegalArgumentException("Internal error: Unhandled generation task type.");
                                        }
                                    },
                                    MDC_KEY_OPERATION, "processQuery." + taskType.name().toLowerCase(),
                                    MDC_KEY_TASK_TYPE_INTERNAL, taskType.name()
                            );
                        }
                        recordMethodDuration(sample, TIMER_PROCESS_QUERY, operationNameForMetrics, generationType.name(), true, null);
                        return result;
                    } catch (Exception e) {
                        incrementErrorCounter(operationNameForMetrics, e, generationType.name());
                        recordMethodDuration(sample, TIMER_PROCESS_QUERY, operationNameForMetrics, generationType.name(), false, e.getClass().getSimpleName());
                        if (e instanceof MLException) throw (MLException) e;
                        if (e instanceof IllegalArgumentException) throw (IllegalArgumentException) e;
                        throw new MLException("Error processing query for type " + generationType + ": " + e.getMessage(), 500, e);
                    }
                },
                MDC_KEY_OPERATION, "processQuery",
                MDC_KEY_GENERATION_TYPE_PARAM, generationType.name(),
                MDC_KEY_QUERY_PREVIEW, query != null ? query.substring(0, Math.min(query.length(), 50)) + "..." : "null"
        );
    }

    @Override
    public String generateGeneralAnswer(String userQuery, String companyDescription, String clientPreviousMessages) throws MLException {
        return MdcUtil.withContext(
                () -> {
                    Timer.Sample sample = Timer.start(meterRegistry);
                    String operationNameForMetrics = "generateGeneralAnswer";
                    incrementRequestCounter(operationNameForMetrics);
                    MLParamsConfig.GenerationTaskType taskType = MLParamsConfig.GenerationTaskType.GENERAL_ANSWER;

                    try {
                        if (companyDescription == null || companyDescription.trim().isEmpty()) {
                            throw new MLException("Company description is required for general answer generation", 400);
                        }

                        String prompt = PromptBuilderService.buildGeneralAnswerPrompt(userQuery, companyDescription, clientPreviousMessages);
                        log.debug("Generated prompt for general answer: {}", prompt);

                        GenerationRequest request = createRequestForTask(prompt, taskType);

                        GenerationResponse response = resilientApiClient.generateText(request, "general_answer_api_call", taskType.name());

                        String generatedText = extractProcessedText(response, "", "general_answer_extraction", taskType.name());
                        recordMethodDuration(sample, TIMER_GENERATE_GENERAL_ANSWER, operationNameForMetrics, taskType.name(), true, null);
                        return generatedText;

                    } catch (Exception e) {
                        incrementErrorCounter(operationNameForMetrics, e, taskType.name());
                        recordMethodDuration(sample, TIMER_GENERATE_GENERAL_ANSWER, operationNameForMetrics, taskType.name(), false, e.getClass().getSimpleName());
                        if (e instanceof MLException) throw (MLException) e;
                        throw new MLException("Error generating general answer: " + e.getMessage(), 500, e);
                    }
                },
                MDC_KEY_OPERATION, "generateGeneralAnswer",
                MDC_KEY_TASK_TYPE_INTERNAL, MLParamsConfig.GenerationTaskType.GENERAL_ANSWER.name(),
                "userQueryPreview", userQuery != null ? userQuery.substring(0, Math.min(userQuery.length(), 50)) + "..." : "null"
        );
    }

    @Override
    public String analyzeClientStyle(String clientMessages) throws MLException {
        return MdcUtil.withContext(
                () -> {
                    Timer.Sample sample = Timer.start(meterRegistry);
                    String operationNameForMetrics = "analyzeClientStyle";
                    incrementRequestCounter(operationNameForMetrics);

                    if (clientMessages == null || clientMessages.trim().isEmpty()) {
                        log.info("Client messages are empty, cannot analyze style.");
                        recordMethodDuration(sample, TIMER_ANALYZE_CLIENT_STYLE, operationNameForMetrics, "N/A_EMPTY_INPUT", true, null);
                        return "";
                    }

                    try {
                        String prompt = PromptBuilderService.buildClientStyleAnalysisPrompt(clientMessages);
                        if (prompt.isEmpty()) {
                            log.warn("Generated prompt for client style analysis is empty. Client messages: {}", clientMessages.substring(0, Math.min(clientMessages.length(),100)));
                            recordMethodDuration(sample, TIMER_ANALYZE_CLIENT_STYLE, operationNameForMetrics, "N/A_EMPTY_PROMPT", true, null);
                            return "";
                        }
                        log.debug("Prompt for client style analysis: {}", prompt.substring(0, Math.min(prompt.length(), 300)) + "...");


                        GenerationRequest request = GenerationRequest.builder()
                                .prompt(prompt)
                                .temperature(apiParams.getTemperatureForTask(MLParamsConfig.GenerationTaskType.GENERAL_ANSWER))
                                .maxNewTokens(100)
                                .topP(apiParams.getTopPForTask(MLParamsConfig.GenerationTaskType.GENERAL_ANSWER))
                                .doSample(apiParams.isDoSampleForTask(MLParamsConfig.GenerationTaskType.GENERAL_ANSWER))
                                .stream(false)
                                .isTextGeneration(true)
                                .build();

                        GenerationResponse response = resilientApiClient.generateText(request, "style_analysis_api_call", "STYLE_ANALYSIS");

                        String styleDescription = extractProcessedText(response, "", "style_analysis_extraction", "STYLE_ANALYSIS");

                        log.info("Analyzed client style: {}", styleDescription);
                        recordMethodDuration(sample, TIMER_ANALYZE_CLIENT_STYLE, operationNameForMetrics, "STYLE_ANALYSIS", true, null);
                        return styleDescription;

                    } catch (Exception e) {
                        incrementErrorCounter(operationNameForMetrics, e, "STYLE_ANALYSIS");
                        recordMethodDuration(sample, TIMER_ANALYZE_CLIENT_STYLE, operationNameForMetrics, "STYLE_ANALYSIS", false, e.getClass().getSimpleName());
                        if (e instanceof MLException) throw (MLException) e;
                        throw new MLException("Error analyzing client style: " + e.getMessage(), 500, e);
                    }
                },
                MDC_KEY_OPERATION, "analyzeClientStyle",
                MDC_KEY_CLIENT_MESSAGES_PREVIEW, clientMessages != null ? clientMessages.substring(0, Math.min(clientMessages.length(), 100)) + "..." : "null"
        );
    }

    private MLParamsConfig.GenerationTaskType mapToInternalTaskType(GenerationType externalType) {
        return switch (externalType) {
            case CORRECTION -> MLParamsConfig.GenerationTaskType.CORRECTION;
            case REWRITE -> MLParamsConfig.GenerationTaskType.REWRITE;
            default -> throw new IllegalArgumentException("Cannot map external GenerationType '" + externalType + "' to an internal GenerationTaskType for single processing step.");
        };
    }

    private String processCorrectionInternal(String query) throws MLException {
        String prompt = PromptBuilderService.buildCorrectionPrompt(query);
        GenerationRequest request = createRequestForTask(prompt, MLParamsConfig.GenerationTaskType.CORRECTION);
        GenerationResponse response = resilientApiClient.generateText(request, "correction_api_call", GenerationType.CORRECTION.name());
        return extractProcessedText(response, query, "correction_extraction", GenerationType.CORRECTION.name());
    }

    private String processRewriteInternal(String query, String clientPreviousMessages) throws MLException {
        String prompt = PromptBuilderService.buildRewritePrompt(query, clientPreviousMessages);
        GenerationRequest request = createRequestForTask(prompt, MLParamsConfig.GenerationTaskType.REWRITE);
        GenerationResponse response = resilientApiClient.generateText(request, "rewrite_api_call", GenerationType.REWRITE.name());
        return extractProcessedText(response, query, "rewrite_extraction", GenerationType.REWRITE.name());
    }

    private GenerationRequest createRequestForTask(String prompt, MLParamsConfig.GenerationTaskType taskType) {
        return GenerationRequest.builder()
                .prompt(prompt)
                .temperature(apiParams.getTemperatureForTask(taskType))
                .maxNewTokens(apiParams.getMaxNewTokensForTask(taskType))
                .topP(apiParams.getTopPForTask(taskType))
                .doSample(apiParams.isDoSampleForTask(taskType))
                .stream(apiParams.isStreamForTask(taskType))
                .isTextGeneration(apiParams.isTextGenerationForTask(taskType))
                .build();
    }

    private String extractProcessedText(GenerationResponse response, String fallbackText, String taskNameForMetrics, String generationTypeForMetrics) {
        if (response == null || response.getGeneratedText() == null || response.getGeneratedText().trim().isEmpty()) {
            log.warn("Empty response received for task '{}', generationType '{}', returning fallback text", taskNameForMetrics, generationTypeForMetrics);
            metricsService.incrementEmptyApiResponse(taskNameForMetrics, generationTypeForMetrics);
            return fallbackText.trim();
        }
        return response.getGeneratedText().trim();
    }

    private void validateInput(String query) throws IllegalArgumentException {
        if (query == null || query.trim().isEmpty()) {
            log.warn("Received null or empty client query");
            throw new IllegalArgumentException("Client query cannot be null or empty");
        }
    }

    private void incrementRequestCounter(String methodNameTagValue) {
        meterRegistry.counter(COUNTER_REQUESTS_TOTAL, TAG_METHOD_NAME, methodNameTagValue).increment();
    }

    private void incrementErrorCounter(String methodNameTagValue, Throwable throwable, String generationTypeTagValue) {
        meterRegistry.counter(COUNTER_ERRORS_TOTAL,
                TAG_METHOD_NAME, methodNameTagValue,
                TAG_EXCEPTION_TYPE, throwable.getClass().getSimpleName(),
                TAG_GENERATION_TYPE, generationTypeTagValue != null ? generationTypeTagValue : "N/A"
        ).increment();
    }

    private void recordMethodDuration(Timer.Sample sample, String timerName, String methodNameTagValue, String generationTypeTagValue, boolean success, String exceptionTypeTagValue) {
        String outcomeTagValue = success ? "SUCCESS" : "FAILURE";

        Timer.Builder timerBuilder = Timer.builder(timerName)
                .tag(TAG_METHOD_NAME, methodNameTagValue)
                .tag(TAG_GENERATION_TYPE, generationTypeTagValue != null ? generationTypeTagValue : "unknown")
                .tag(TAG_OUTCOME, outcomeTagValue);

        if (!success && exceptionTypeTagValue != null) {
            timerBuilder.tag(TAG_EXCEPTION_TYPE, exceptionTypeTagValue);
        }

        sample.stop(timerBuilder.register(meterRegistry));
    }
}

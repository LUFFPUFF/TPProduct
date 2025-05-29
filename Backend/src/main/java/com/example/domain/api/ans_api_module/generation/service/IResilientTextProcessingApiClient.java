package com.example.domain.api.ans_api_module.generation.service;

import com.example.domain.api.ans_api_module.generation.model.GenerationRequest;
import com.example.domain.api.ans_api_module.generation.model.GenerationResponse;
import com.example.domain.api.ans_api_module.generation.exception.MLException;

public interface IResilientTextProcessingApiClient {

    GenerationResponse generateText(GenerationRequest request, String taskName, String generationType) throws MLException;
}

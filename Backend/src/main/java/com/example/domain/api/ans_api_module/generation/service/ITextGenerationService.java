package com.example.domain.api.ans_api_module.generation.service;

import com.example.domain.api.ans_api_module.generation.exception.MLException;
import com.example.domain.api.ans_api_module.generation.model.enums.GenerationType;

public interface ITextGenerationService {

    String processQuery(String query, GenerationType generationType, String clientPreviousMessages) throws MLException;
    String generateGeneralAnswer(String userQuery, String companyDescription, String clientPreviousMessages) throws MLException;
    String analyzeClientStyle(String clientMessages) throws MLException;
}

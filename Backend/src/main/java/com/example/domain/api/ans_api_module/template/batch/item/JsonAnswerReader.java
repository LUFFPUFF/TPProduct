package com.example.domain.api.ans_api_module.template.batch.item;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.repository.company_subscription_module.CompanyRepository;
import com.example.domain.api.ans_api_module.template.exception.FileProcessingException;
import com.example.domain.api.ans_api_module.template.exception.TextProcessingException;
import com.example.domain.api.ans_api_module.template.util.FileType;
import com.example.domain.api.ans_api_module.template.util.ValidationUtils;
import com.example.domain.api.ans_api_module.template.dto.request.PredefinedAnswerUploadDto;
import com.example.domain.dto.company_module.CompanyDto;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.common.util.StringUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@StepScope
public class JsonAnswerReader implements AnswerFileReader {

    private static final long MAX_JSON_RECORDS = 10_000;

    private final ObjectMapper objectMapper;
    private final ValidationUtils validationUtils;
    private final CompanyRepository companyRepository;

    @Value("#{jobParameters['companyId']}")
    private Long jobCompanyId;

    @Value("#{jobParameters['category']}")
    private String jobCategory;

    @Override
    public List<PredefinedAnswerUploadDto> read(File file) {
        List<PredefinedAnswerUploadDto> result = new ArrayList<>(500);
        JsonFactory jsonFactory = objectMapper.getFactory();

        // Получаем компанию один раз перед обработкой
        Company company = companyRepository.findById(Math.toIntExact(jobCompanyId))
                .orElseThrow(() -> new EntityNotFoundException(
                        "Company with id " + jobCompanyId + " not found"));

        CompanyDto companyDto = CompanyDto.builder()
                .id(company.getId())
                .name(company.getName())
                .contactEmail(company.getContactEmail())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();

        try (InputStream is = new FileInputStream(file);
             JsonParser parser = jsonFactory.createParser(is)) {

            if (file.length() == 0) {
                throw new FileProcessingException("JSON file is empty");
            }

            parser.configure(JsonParser.Feature.STRICT_DUPLICATE_DETECTION, true);

            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IllegalStateException("Expected JSON array as root element");
            }

            long recordCount = 0;

            while (parser.nextToken() != JsonToken.END_ARRAY) {
                if (parser.currentToken() == JsonToken.START_OBJECT) {
                    if (++recordCount > MAX_JSON_RECORDS) {
                        throw new IllegalStateException("Max records limit exceeded: " + MAX_JSON_RECORDS);
                    }

                    JsonNode node = objectMapper.readTree(parser);
                    PredefinedAnswerUploadDto dto = parseJsonNode(node, companyDto, recordCount);

                    validationUtils.validateAnswerDto(dto);
                    result.add(dto);
                }
            }
        } catch (Exception e) {
            throw new FileProcessingException("Failed to process JSON file", e);
        }

        return result;
    }

    private PredefinedAnswerUploadDto parseJsonNode(JsonNode node, CompanyDto companyDto, long recordNumber) {
        PredefinedAnswerUploadDto dto = new PredefinedAnswerUploadDto();
        dto.setCompanyDto(companyDto);

        if (!node.has("title") || node.get("title").isNull()) {
            throw new TextProcessingException(STR."Title is required for answer at position \{recordNumber}");
        }
        dto.setTitle(node.get("title").asText().trim());

        if (!node.has("answer") || node.get("answer").isNull()) {
            throw new TextProcessingException(STR."Answer is required for answer at position \{recordNumber}");
        }
        dto.setAnswer(node.get("answer").asText().trim());

        if (node.has("category") && !node.get("category").isNull()) {
            dto.setCategory(node.get("category").asText().trim());
        } else if (jobCategory != null) {
            dto.setCategory(jobCategory);
        }

        return dto;
    }

    @Override
    public boolean supports(FileType fileType) {
        return fileType == FileType.JSON;
    }
}

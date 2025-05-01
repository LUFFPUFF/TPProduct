package com.example.domain.api.ans_api_module.template.batch.item;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.repository.company_subscription_module.CompanyRepository;
import com.example.domain.api.ans_api_module.template.exception.FileProcessingException;
import com.example.domain.api.ans_api_module.template.exception.TextProcessingException;
import com.example.domain.api.ans_api_module.template.util.FileType;
import com.example.domain.api.ans_api_module.template.util.ValidationUtils;
import com.example.domain.api.ans_api_module.template.dto.request.PredefinedAnswerUploadDto;
import com.example.domain.dto.CompanyDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@StepScope
public class TxtAnswerReader implements AnswerFileReader {

    private static final Pattern TXT_LINE_PATTERN = Pattern.compile(
            "^(\\d+)\\|([^|]+)\\|([^|]+)\\|([^|]+)\\|?.*$"
    );
    private static final int MAX_LINE_LENGTH = 10_000;

    private final ValidationUtils validationUtils;
    private final CompanyRepository companyRepository;

    @Value("#{jobParameters['companyId']}")
    private Long jobCompanyId;

    @Value("#{jobParameters['category']}")
    private String jobCategory;

    @Override
    public List<PredefinedAnswerUploadDto> read(File file) {
        List<PredefinedAnswerUploadDto> result = new ArrayList<>(500);
        long lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            Company company = companyRepository.findById(Math.toIntExact(jobCompanyId))
                    .orElseThrow(() -> new EntityNotFoundException("Company with id " + jobCompanyId + " not found"));

            CompanyDto companyDto = CompanyDto.builder()
                    .id(company.getId())
                    .name(company.getName())
                    .contactEmail(company.getContactEmail())
                    .createdAt(company.getCreatedAt())
                    .updatedAt(company.getUpdatedAt())
                    .build();

            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                try {
                    if (line.length() > MAX_LINE_LENGTH) {
                        throw new TextProcessingException(
                                "Line too long at " + lineNumber + ", max allowed: " + MAX_LINE_LENGTH);
                    }

                    if (!line.trim().isEmpty()) {
                        PredefinedAnswerUploadDto dto = parseLine(line, companyDto);
                        validationUtils.validateAnswerDto(dto);
                        result.add(dto);
                    }
                } catch (Exception e) {
                    throw new TextProcessingException(
                            "Error processing line " + lineNumber + ": " + line, e);
                }
            }
        } catch (TextProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new FileProcessingException("Failed to read TXT file", e);
        }

        return result;
    }

    private PredefinedAnswerUploadDto parseLine(String line, CompanyDto companyDto) {
        var matcher = TXT_LINE_PATTERN.matcher(line);
        if (!matcher.matches()) {
            throw new TextProcessingException("Invalid line format. Expected: companyId|category|question|answer");
        }

        String category = matcher.group(2).trim();
        if (category.isEmpty() && StringUtils.hasText(jobCategory)) {
            category = jobCategory;
        }

        return PredefinedAnswerUploadDto.builder()
                .companyDto(companyDto)
                .category(category)
                .title(matcher.group(3).trim())
                .answer(matcher.group(4).trim())
                .build();
    }

    @Override
    public boolean supports(FileType fileType) {
        return fileType == FileType.TXT;
    }
}

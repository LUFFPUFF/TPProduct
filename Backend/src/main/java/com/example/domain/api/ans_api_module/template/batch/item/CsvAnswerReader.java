package com.example.domain.api.ans_api_module.template.batch.item;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.repository.company_subscription_module.CompanyRepository;
import com.example.domain.api.ans_api_module.template.exception.CsvProcessingException;
import com.example.domain.api.ans_api_module.template.exception.FileProcessingException;
import com.example.domain.api.ans_api_module.template.exception.TextProcessingException;
import com.example.domain.api.ans_api_module.template.exception.ValidationException;
import com.example.domain.api.ans_api_module.template.util.FileType;
import com.example.domain.api.ans_api_module.template.util.ValidationUtils;
import com.example.domain.api.ans_api_module.template.dto.request.PredefinedAnswerUploadDto;
import com.example.domain.dto.CompanyDto;
import io.micrometer.common.util.StringUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@RequiredArgsConstructor
@StepScope
public class CsvAnswerReader implements AnswerFileReader {

    private static final String[] HEADERS = {
            "category", "title", "answer", "templateCode", "tags"
    };

    private final ValidationUtils validationUtils;
    private final CompanyRepository companyRepository;

    @Value("#{jobParameters['companyId']}")
    private Long jobCompanyId;

    @Value("#{jobParameters['category']}")
    private String jobCategory;

    @Override
    public List<PredefinedAnswerUploadDto> read(File file) {
        List<PredefinedAnswerUploadDto> result = new ArrayList<>(500);

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

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader(HEADERS)
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .setAllowMissingColumnNames(false)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                try {
                    PredefinedAnswerUploadDto dto = mapRecordToDto(record, companyDto);

                    if (StringUtils.isBlank(dto.getTitle())) {
                        throw new TextProcessingException(
                                "Title is required for answer at record #" + record.getRecordNumber());
                    }

                    if (StringUtils.isBlank(dto.getAnswer())) {
                        throw new TextProcessingException(
                                "Answer is required for answer at record #" + record.getRecordNumber());
                    }

                    if (StringUtils.isBlank(dto.getCategory()) && StringUtils.isNotBlank(jobCategory)) {
                        dto.setCategory(jobCategory);
                    }

                    validationUtils.validateAnswerDto(dto);
                    result.add(dto);
                } catch (Exception e) {
                    throw new CsvProcessingException(
                            "Error processing CSV record #" + record.getRecordNumber(),
                            record.toMap(),
                            e);
                }
            }
        } catch (CsvProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new FileProcessingException("CSV file processing failed", e);
        }

        return result;
    }

    private PredefinedAnswerUploadDto mapRecordToDto(CSVRecord record, CompanyDto companyDto) {
        return PredefinedAnswerUploadDto.builder()
                .companyDto(companyDto)
                .category(getCsvValue(record, "category"))
                .title(getCsvValue(record, "title"))
                .answer(getCsvValue(record, "answer"))
                .build();
    }

    private String getCsvValue(CSVRecord record, String column) {
        try {
            return record.get(column);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public boolean supports(FileType fileType) {
        return fileType == FileType.CSV;
    }
}

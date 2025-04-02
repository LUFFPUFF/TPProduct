package com.example.domain.api.ans_api_module.template.batch.item;

import com.example.domain.api.ans_api_module.template.exception.CsvProcessingException;
import com.example.domain.api.ans_api_module.template.exception.FileProcessingException;
import com.example.domain.api.ans_api_module.template.exception.ValidationException;
import com.example.domain.api.ans_api_module.template.util.FileType;
import com.example.domain.api.ans_api_module.template.util.ValidationUtils;
import com.example.domain.dto.ans_module.predefined_answer.request.PredefinedAnswerUploadDto;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CsvAnswerReader implements AnswerFileReader {

    private static final String[] HEADERS = {
            "companyId", "category", "title", "answer",
            "templateCode", "tags"
    };

    private static final String TAG_DELIMITER = "\\|";
    private static final int MAX_TAGS = 10;

    private final ValidationUtils validationUtils;

    @Override
    public List<PredefinedAnswerUploadDto> read(MultipartFile file) {
        List<PredefinedAnswerUploadDto> result = new ArrayList<>(500);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));

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
                    PredefinedAnswerUploadDto dto = mapRecordToDto(record);
                    validationUtils.validateAnswerDto(dto);
                    result.add(dto);
                } catch (Exception e) {
                    throw new CsvProcessingException(
                            STR."Error processing CSV record #\{record.getRecordNumber()}",
                            record.toMap(),
                            e
                    );
                }
            }
        } catch (CsvProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new FileProcessingException("CSV file processing failed", e);
        }

        return result;
    }

    private PredefinedAnswerUploadDto mapRecordToDto(CSVRecord record) {
        return new PredefinedAnswerUploadDto(
                parseCompanyId(record.get("companyId")),
                record.get("category"),
                record.get("title"),
                record.get("answer"),
                parseTemplateCode(record.get("templateCode")),
                parseTags(record.get("tags"))
        );
    }

    private Integer parseCompanyId(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(STR."Invalid companyId: \{value}");
        }
    }

    private String parseTemplateCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
    }

    private Set<String> parseTags(String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptySet();
        }

        return Arrays.stream(value.split(TAG_DELIMITER))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .limit(MAX_TAGS)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public boolean supports(FileType fileType) {
        return fileType == FileType.CSV;
    }
}

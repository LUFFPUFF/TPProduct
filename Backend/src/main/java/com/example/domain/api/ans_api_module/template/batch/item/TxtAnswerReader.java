package com.example.domain.api.ans_api_module.template.batch.item;

import com.example.domain.api.ans_api_module.template.exception.FileProcessingException;
import com.example.domain.api.ans_api_module.template.exception.TextProcessingException;
import com.example.domain.api.ans_api_module.template.util.FileType;
import com.example.domain.api.ans_api_module.template.util.ValidationUtils;
import com.example.domain.dto.ans_module.predefined_answer.request.PredefinedAnswerUploadDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TxtAnswerReader implements AnswerFileReader {

    private static final Pattern TXT_LINE_PATTERN = Pattern.compile(
            "^([^|]+)\\|([^|]+)\\|([^|]+)\\|([^|]+)\\|([^|]*)\\|([^|]*)$"
    );
    private static final int MAX_LINE_LENGTH = 10_000;
    private static final int MAX_TAGS = 10;

    private final ValidationUtils validationUtils;

    @Override
    public List<PredefinedAnswerUploadDto> read(MultipartFile file) {
        List<PredefinedAnswerUploadDto> result = new ArrayList<>(500);
        long lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.length() > MAX_LINE_LENGTH) {
                    throw new TextProcessingException(
                            "Line too long at " + lineNumber + ", max allowed: " + MAX_LINE_LENGTH);
                }

                if (!line.trim().isEmpty()) {
                    try {
                        PredefinedAnswerUploadDto dto = parseLine(line);
                        validationUtils.validateAnswerDto(dto);
                        result.add(dto);
                    } catch (Exception e) {
                        throw new TextProcessingException(
                                "Error processing line " + lineNumber + ": " + line, e);
                    }
                }
            }
        } catch (TextProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new FileProcessingException("Failed to read TXT file", e);
        }

        return result;
    }

    private PredefinedAnswerUploadDto parseLine(String line) {
        var matcher = TXT_LINE_PATTERN.matcher(line);
        if (!matcher.matches()) {
            throw new TextProcessingException("Invalid line format");
        }

        return new PredefinedAnswerUploadDto(
                Integer.parseInt(matcher.group(1).trim()),
                matcher.group(2).trim(),
                matcher.group(3).trim(),
                matcher.group(4).trim(),
                matcher.group(5).isBlank() ? null : matcher.group(5).trim().toUpperCase(),
                parseTags(matcher.group(6))
        );
    }

    private Set<String> parseTags(String tagsString) {
        if (tagsString == null || tagsString.isBlank()) {
            return Collections.emptySet();
        }

        return Arrays.stream(tagsString.split("\\|"))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .limit(MAX_TAGS)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public boolean supports(FileType fileType) {
        return fileType == FileType.TXT;
    }
}

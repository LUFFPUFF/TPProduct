package com.example.domain.api.ans_api_module.template.batch.item;

import com.example.domain.api.ans_api_module.template.exception.FileProcessingException;
import com.example.domain.api.ans_api_module.template.util.FileType;
import com.example.domain.api.ans_api_module.template.util.ValidationUtils;
import com.example.domain.dto.ans_module.predefined_answer.request.PredefinedAnswerUploadDto;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JsonAnswerReader implements AnswerFileReader {

    private static final int MAX_JSON_DEPTH = 5;
    private static final long MAX_JSON_RECORDS = 10_000;

    private final ObjectMapper objectMapper;
    private final ValidationUtils validationUtils;

    @Override
    public List<PredefinedAnswerUploadDto> read(MultipartFile file) {
        List<PredefinedAnswerUploadDto> result = new ArrayList<>(500);
        JsonFactory jsonFactory = objectMapper.getFactory();

        try (InputStream is = file.getInputStream();
             JsonParser parser = jsonFactory.createParser(is)) {

            parser.configure(JsonParser.Feature.STRICT_DUPLICATE_DETECTION, true);

            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IllegalStateException("Expected JSON array as root element");
            }

            long recordCount = 0;
            int currentDepth;

            while (parser.nextToken() != JsonToken.END_ARRAY) {
                if (parser.currentToken() == JsonToken.START_OBJECT) {
                    currentDepth = calculateNestingLevel(parser);
                    if (currentDepth > MAX_JSON_DEPTH) {
                        throw new IllegalStateException(STR."JSON nesting depth exceeded maximum limit of \{MAX_JSON_DEPTH}");
                    }

                    if (++recordCount > MAX_JSON_RECORDS) {
                        throw new IllegalStateException(STR."Max records limit exceeded: \{MAX_JSON_RECORDS}");
                    }

                    PredefinedAnswerUploadDto dto = objectMapper.readValue(parser, PredefinedAnswerUploadDto.class);
                    validationUtils.validateAnswerDto(dto);
                    result.add(dto);
                }
            }
        } catch (Exception e) {
            throw new FileProcessingException("Failed to process JSON file", e);
        }

        return result;
    }

    private int calculateNestingLevel(JsonParser parser) {
        int depth = 0;
        JsonToken token;
        while ((token = parser.currentToken()) != null) {
            if (token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY) {
                depth++;
            } else if (token == JsonToken.END_OBJECT || token == JsonToken.END_ARRAY) {
                depth--;
            }
            if (depth < 0) break;
        }
        return depth;
    }

    @Override
    public boolean supports(FileType fileType) {
        return false;
    }
}

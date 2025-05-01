package com.example.domain.api.ans_api_module.correction_answer.controller;

import com.example.domain.api.ans_api_module.correction_answer.dto.textProcessingDto.Status;
import com.example.domain.api.ans_api_module.correction_answer.dto.textProcessingDto.TextProcessingRequest;
import com.example.domain.api.ans_api_module.correction_answer.dto.textProcessingDto.TextProcessingResponse;
import com.example.domain.api.ans_api_module.correction_answer.exception.MLException;
import com.example.domain.api.ans_api_module.correction_answer.service.GenerationType;
import com.example.domain.api.ans_api_module.correction_answer.service.TextProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/text")
@RequiredArgsConstructor
@Slf4j
@Validated
public class TextProcessingController {

    private final TextProcessingService textProcessingService;

    @PostMapping("/process")
    public ResponseEntity<TextProcessingResponse> processText(
            @RequestBody @Valid TextProcessingRequest request,
            @RequestParam(defaultValue = "CORRECTION") GenerationType generationType) {

        log.info("Received text processing request: type={}, length={}",
                generationType, request.text().length());

        try {
            String processedText = textProcessingService.processQuery(
                    request.text(),
                    generationType);

            return ResponseEntity.ok(
                    new TextProcessingResponse(
                            processedText,
                            "Text processed successfully",
                            Status.SUCCESS
                    ));
        } catch (MLException e) {
            log.error("ML processing error: {}", e.getMessage(), e);
            return ResponseEntity.status(e.getStatusCode())
                    .body(new TextProcessingResponse(
                            request.text(),
                            "ML processing error: " + e.getMessage(),
                            Status.ERROR
                    ));
        } catch (Exception e) {
            log.error("Unexpected error processing text", e);
            return ResponseEntity.internalServerError()
                    .body(new TextProcessingResponse(
                            request.text(),
                            "Internal server error",
                            Status.ERROR
                    ));
        }
    }
}

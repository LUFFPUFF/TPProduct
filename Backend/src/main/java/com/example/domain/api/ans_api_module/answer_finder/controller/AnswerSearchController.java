package com.example.domain.api.ans_api_module.answer_finder.controller;

import com.example.domain.api.ans_api_module.answer_finder.dto.AnswerSearchResultItem;
import com.example.domain.api.ans_api_module.answer_finder.dto.rest.AnswerSearchRequest;
import com.example.domain.api.ans_api_module.answer_finder.dto.rest.AnswerSearchResponse;
import com.example.domain.api.ans_api_module.answer_finder.exception.AnswerSearchException;
import com.example.domain.api.ans_api_module.answer_finder.service.AnswerSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/answers")
@Slf4j
public class AnswerSearchController {

    private final AnswerSearchService answerSearchService;

    public AnswerSearchController(AnswerSearchService answerSearchService) {
        this.answerSearchService = answerSearchService;
    }

    @PostMapping("/search")
    public ResponseEntity<AnswerSearchResponse> searchAnswers(@Validated @RequestBody AnswerSearchRequest request) {
        List<AnswerSearchResultItem> resultItems;
        try {
            resultItems = answerSearchService.findRelevantAnswers(
                    request.getClientQuery(),
                    request.getCompanyId(),
                    request.getCategory()
            );

            AnswerSearchResponse answerSearchResponse = AnswerSearchResponse.builder()
                    .results(resultItems)
                    .build();

            return ResponseEntity.ok(answerSearchResponse);
        } catch (AnswerSearchException e) {
            log.error("Ошибка бизнес-логики при поиске ответов:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        } catch (Exception e) {
            log.error("Непредвиденная ошибка при поиске ответов:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
}

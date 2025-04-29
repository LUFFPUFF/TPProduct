package com.example.domain.api.ans_api_module.answer_finder.dto.rest;

import com.example.domain.api.ans_api_module.answer_finder.dto.AnswerSearchResultItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerSearchResponse {

    private List<AnswerSearchResultItem> results = Collections.emptyList();
}

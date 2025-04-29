package com.example.domain.api.ans_api_module.answer_finder.dto;

import com.example.domain.api.ans_api_module.answer_finder.domain.dto.PredefinedAnswerDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerSearchResultItem {

    private PredefinedAnswerDto answer;
    private double score;

    //TODO в будущем можно доработать и добавить String highlightedAnswer -
    // Ответ с подсвеченными ключевыми словами и matchedKeywords - Список ключевых слов, по которым найдено совпадение

}

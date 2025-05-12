package com.example.domain.api.ans_api_module.answer_finder.dto.rest;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerSearchRequest {

    private String clientQuery;
    private Integer companyId;
    private String category;

}

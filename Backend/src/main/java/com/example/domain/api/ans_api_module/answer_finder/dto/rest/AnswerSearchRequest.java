package com.example.domain.api.ans_api_module.answer_finder.dto.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerSearchRequest {

    @NotBlank
    @Size(max = 1000)
    private String clientQuery;
    private Integer companyId;
    private String category;

}

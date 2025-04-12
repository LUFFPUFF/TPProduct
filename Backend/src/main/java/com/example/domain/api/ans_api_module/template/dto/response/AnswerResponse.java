package com.example.domain.api.ans_api_module.template.dto.response;

import lombok.Data;

import java.time.Instant;

@Data
public class AnswerResponse {
    private Integer id;
    private String title;
    private String answer;
    private String category;
    private String companyName;
    private Instant createdAt;
    private boolean isActive;

    public AnswerResponse() {
    }

    public AnswerResponse(Integer id, String title, String answer, String category, String companyName, Instant createdAt,boolean isActive) {
        this.id = id;
        this.title = title;
        this.answer = answer;
        this.category = category;
        this.companyName = companyName;
        this.createdAt = createdAt;
        this.isActive = isActive;
    }
}

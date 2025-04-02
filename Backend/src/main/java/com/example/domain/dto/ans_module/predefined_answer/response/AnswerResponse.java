package com.example.domain.dto.ans_module.predefined_answer.response;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class AnswerResponse {
    private Long id;
    private String title;
    private String answer;
    private String category;
    private String templateCode;
    private String companyName;
    private Instant createdAt;
    private boolean isActive;

    public AnswerResponse() {
    }

    public AnswerResponse(Long id, String title, String answer, String category,
                          String templateCode, String companyName, Instant createdAt,boolean isActive) {
        this.id = id;
        this.title = title;
        this.answer = answer;
        this.category = category;
        this.templateCode = templateCode;
        this.companyName = companyName;
        this.createdAt = createdAt;
        this.isActive = isActive;
    }
}

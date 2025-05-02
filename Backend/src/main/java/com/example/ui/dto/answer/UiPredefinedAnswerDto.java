package com.example.ui.dto.answer;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class UiPredefinedAnswerDto {

    private Integer id;
    private String title;
    private String answer;
    private String category;
    private String companyName;
    private Instant createdAt;
}

package com.example.domain.api.ans_api_module.answer_finder.domain.dto;

import com.example.domain.dto.company_module.CompanyDto;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PredefinedAnswerDto {
    private Integer id;
    private CompanyDto company;
    private String title;
    private String answer;
    private double trustScore;
    private LocalDateTime createdAt;
}

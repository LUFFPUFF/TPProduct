package com.example.domain.dto.ans_module;

import com.example.domain.dto.company_module.CompanyDto;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PredefinedAnswerDto {

    private Integer id;
    private CompanyDto companyDto;
    private String category;
    private String title;
    private String answer;
    private LocalDateTime createdAt;
}

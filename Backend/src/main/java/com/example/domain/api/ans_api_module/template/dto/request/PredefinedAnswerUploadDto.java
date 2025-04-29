package com.example.domain.api.ans_api_module.template.dto.request;

import com.example.domain.dto.CompanyDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;


@Getter
@Setter
@ToString
@Builder
public class PredefinedAnswerUploadDto {

    @NotNull(message = "Company DTO cannot be null")
    private CompanyDto companyDto;

    @NotBlank(message = "Category cannot be blank")
    private String category;

    @NotBlank(message = "Title cannot be blank")
    private String title;

    @NotBlank(message = "Answer cannot be blank")
    private String answer;

    public PredefinedAnswerUploadDto() {
    }

    public PredefinedAnswerUploadDto(CompanyDto companyDto, String category, String title, String answer) {
        this.companyDto = companyDto;
        this.category = category;
        this.title = title;
        this.answer = answer;
    }
}

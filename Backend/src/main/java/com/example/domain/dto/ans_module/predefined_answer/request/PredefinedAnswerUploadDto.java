package com.example.domain.dto.ans_module.predefined_answer.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class PredefinedAnswerUploadDto {

    @NotNull(message = "Company ID cannot be null")
    private Integer companyId;

    @NotBlank(message = "Category cannot be blank")
    private String category;

    @NotBlank(message = "Title cannot be blank")
    private String title;

    @NotBlank(message = "Answer cannot be blank")
    private String answer;

    public PredefinedAnswerUploadDto() {
    }

    public PredefinedAnswerUploadDto(Integer companyId, String category, String title,
                                     String answer) {
        this.companyId = companyId;
        this.category = category;
        this.title = title;
        this.answer = answer;
    }
}

package com.example.ui.dto.answer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UiPredefinedAnswerRequest {

    @NotBlank(message = "Title cannot be blank")
    private String title;
    @NotBlank(message = "Answer cannot be blank")
    private String answer;
    @NotBlank(message = "Category cannot be blank")
    private String category;
}

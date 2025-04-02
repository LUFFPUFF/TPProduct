package com.example.domain.dto.ans_module.predefined_answer.request;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.*;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadFileRequest {

    @NotNull(message = "File cannot be null")
    private MultipartFile file;

    @NotNull(message = "Company ID cannot be null")
    private Integer companyId;

    @NotNull(message = "Category cannot be null")
    private String category;

    @Builder.Default
    private boolean overwriteExisting = false;
}

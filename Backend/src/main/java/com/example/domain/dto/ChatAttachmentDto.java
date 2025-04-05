package com.example.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data

public class ChatAttachmentDto {
    private Integer id;

    @NotNull(message = "Message ID не может быть пустым")
    private Integer messageId;

    @NotNull(message = "File URL не может быть пустым")
    @Size(max = 255, message = "URL файла не должен превышать 255 символов")
    private String fileUrl;

    @NotNull(message = "Тип файла не может быть пустым")
    @Size(max = 50, message = "Тип файла не должен превышать 50 символов")
    private String fileType;
}

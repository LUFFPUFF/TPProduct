package com.example.domain.dto.chat_module.web_socket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReadStatusDTO {

    private Integer chatId;
    private Integer messageId;
}

package com.example.domain.dto.chat_module.web_socket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDTO {

    private Integer userId;
    private String notification;
}

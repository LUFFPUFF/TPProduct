package com.example.domain.api.chat_service_api.integration.dto.rest;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateDialogXChatConfigurationRequest {

    private Boolean enabled;

    private String welcomeMessage;

    private String themeColor;
}

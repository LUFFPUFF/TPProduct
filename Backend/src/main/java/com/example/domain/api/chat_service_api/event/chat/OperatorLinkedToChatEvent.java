package com.example.domain.api.chat_service_api.event.chat;

import com.example.domain.api.chat_service_api.model.dto.ChatDTO;

public record OperatorLinkedToChatEvent(Object source, ChatDTO chatDto, String operatorPrincipalName, Integer companyId) {}

package com.example.domain.api.chat_service_api.event.chat;

import com.example.domain.api.chat_service_api.model.dto.ChatDTO;

public record ChatEscalatedToOperatorEvent(Object source, ChatDTO chatDto, Integer companyId) {}

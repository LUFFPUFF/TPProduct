package com.example.domain.api.chat_service_api.event.message;

import com.example.domain.api.chat_service_api.model.dto.MessageDto;

public record ChatMessageStatusUpdatedEvent(Object source, MessageDto updatedMessageDto) {}

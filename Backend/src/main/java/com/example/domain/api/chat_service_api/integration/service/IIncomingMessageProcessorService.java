package com.example.domain.api.chat_service_api.integration.service;

import com.example.domain.api.chat_service_api.integration.dto.IncomingChannelMessage;

public interface IIncomingMessageProcessorService {

    void processIncomingMessage(Integer companyId, IncomingChannelMessage message);
}

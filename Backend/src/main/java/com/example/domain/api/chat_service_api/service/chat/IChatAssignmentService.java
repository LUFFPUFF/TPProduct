package com.example.domain.api.chat_service_api.service.chat;

import com.example.domain.api.chat_service_api.model.dto.ChatDetailsDTO;
import com.example.domain.api.chat_service_api.model.rest.chat.AssignChatRequestDTO;
import com.example.domain.security.model.UserContext;

import java.nio.file.AccessDeniedException;

public interface IChatAssignmentService {

    ChatDetailsDTO assignChatToOperator(AssignChatRequestDTO assignRequest, UserContext userContext) throws AccessDeniedException;

    ChatDetailsDTO escalateChatToOperator(Integer chatId, Integer clientId);

    void linkOperatorToChat(Integer chatId, Integer operatorId, UserContext userContext) throws AccessDeniedException;
}

package com.example.domain.api.chat_service_api.service.chat;

import com.example.domain.api.chat_service_api.model.dto.ChatDetailsDTO;
import com.example.domain.api.chat_service_api.model.rest.chat.CreateChatRequestDTO;
import com.example.domain.security.model.UserContext;

import java.nio.file.AccessDeniedException;

public interface IChatCreationService {

    ChatDetailsDTO createChat(CreateChatRequestDTO createRequest);

    ChatDetailsDTO createChatFromOperatorUI(CreateChatRequestDTO createRequest, UserContext userContext) throws AccessDeniedException;

    ChatDetailsDTO createTestChatForCurrentUser(UserContext userContext) throws AccessDeniedException;
}

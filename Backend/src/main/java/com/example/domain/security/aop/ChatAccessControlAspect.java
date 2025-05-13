package com.example.domain.security.aop;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.database.repository.chats_messages_module.ChatMessageRepository;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.security.aop.annotation.CheckChatCompanyAccess;
import com.example.domain.security.model.UserContext;
import com.example.domain.security.util.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.nio.file.AccessDeniedException;
import java.util.Objects;

@Aspect
@Component
@RequiredArgsConstructor
public class ChatAccessControlAspect {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Before("@annotation(checkChatCompanyAccess)")
    public void checkAccess(JoinPoint joinPoint, CheckChatCompanyAccess checkChatCompanyAccess) throws AccessDeniedException {

        UserContext userContext = UserContextHolder.getRequiredContext();
        Integer companyId = userContext.getCompanyId();

        if (companyId == null) {
            throw new AccessDeniedException("Access Denied: User is not associated with a company.");
        }

        String idParamName = checkChatCompanyAccess.idParamName();
        Integer entityId = getInteger(joinPoint, idParamName);

        Integer entityCompanyId = null;
        String lowerCaseIdParamName = idParamName.toLowerCase();

        if (lowerCaseIdParamName.equals("chatid")) {
            Chat chat = chatRepository.findById(entityId)
                    .orElseThrow(() -> new ResourceNotFoundException("Chat with ID " + entityId + " not found."));
            if (chat.getCompany() != null) {
                entityCompanyId = chat.getCompany().getId();
            }
        } else if (lowerCaseIdParamName.contains("messageid")) {
            ChatMessage message = chatMessageRepository.findById(entityId)
                    .orElseThrow(() -> new ResourceNotFoundException("Message with ID " + entityId + " not found."));
            if (message.getChat() != null && message.getChat().getCompany() != null) {
                entityCompanyId = message.getChat().getCompany().getId();
            }
        } else {
            throw new IllegalStateException("Access control aspect misconfiguration: Unsupported ID parameter name '" + idParamName + "'. Expected 'chatId' or 'messageId'.");
        }

        if (entityCompanyId == null || !Objects.equals(companyId, entityCompanyId)) {
            throw new AccessDeniedException(checkChatCompanyAccess.message());
        }

    }

    private static @NotNull Integer getInteger(JoinPoint joinPoint, String idParamName) throws AccessDeniedException {
        Integer entityId = null;
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();

        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(idParamName) && args[i] instanceof Integer) {
                entityId = (Integer) args[i];
                break;
            }
        }

        if (entityId == null) {
            throw new AccessDeniedException("Access Denied: No such entity found.");
        }
        return entityId;
    }
}

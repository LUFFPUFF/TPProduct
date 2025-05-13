package com.example.domain.security.aop;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.repository.chats_messages_module.ChatMessageRepository;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.security.aop.annotation.SecureChatAccess;
import com.example.domain.security.model.UserContext;
import com.example.domain.security.util.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.nio.file.AccessDeniedException;
import java.util.Objects;
import java.util.Set;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class SecureChatAccessAspect {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Before("@annotation(secureChatAccess)")
    public void checkAccess(JoinPoint joinPoint, SecureChatAccess secureChatAccess) throws AccessDeniedException {
        UserContext userContext = UserContextHolder.getRequiredContext();
        Integer currentUserId = userContext.getUserId();
        Integer userCompanyId = userContext.getCompanyId();
        Set<Role> userRoles = userContext.getRoles();

        if (userCompanyId == null) {
            if (!userRoles.contains(Role.MANAGER) && !userRoles.contains(Role.OPERATOR)) {
                throw new AccessDeniedException(secureChatAccess.message() + " User is not associated with a company.");
            }
            throw new AccessDeniedException(secureChatAccess.message() + " User is not associated with a company.");
        }

        String idParamName = secureChatAccess.idParamName();
        String idMethodName = secureChatAccess.idMethodName();
        Integer entityId;
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();

        Object idParameter = null;
        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(idParamName)) {
                idParameter = args[i];
                break;
            }
        }

        if (idParameter == null) {
            throw new IllegalStateException("Access control aspect misconfiguration: Entity ID parameter not found.");
        }

        if (idMethodName.isEmpty()) {
            if (!(idParameter instanceof Integer)) {
                throw new IllegalStateException("Access control aspect misconfiguration: ID parameter is not of expected type.");
            }

            entityId = (Integer) idParameter;
        } else {
            try {
                Method method = idParameter.getClass().getMethod(idMethodName);
                Object idValue = method.invoke(idParameter);
                if (idValue instanceof Integer) {
                    entityId = (Integer) idValue;
                } else {
                    throw new IllegalStateException("Access control aspect misconfiguration: ID method did not return Integer.");
                }
            } catch (Exception e) {
                log.error("SecureChatAccess aspect misconfiguration: Failed to call method '{}' on parameter '{}' for {}.{}: {}",
                        idMethodName, idParamName, signature.getDeclaringTypeName(), signature.getName(), e.getMessage(), e);
                throw new IllegalStateException("Access control aspect misconfiguration: Failed to extract ID from parameter.", e);
            }
        }

        if (entityId == null) {
            throw new IllegalStateException("Access control aspect failed to obtain entity ID.");
        }

        Chat chat;
        String lowerCaseName = idMethodName.isEmpty() ? idParamName.toLowerCase() : idMethodName.toLowerCase();

        if (lowerCaseName.contains("chatid")) {
            chat = chatRepository.findById(entityId)
                    .orElseThrow(() -> new ResourceNotFoundException("Chat with ID " + entityId + " not found."));

        } else if (lowerCaseName.contains("messageid")) {
            ChatMessage message = chatMessageRepository.findById(entityId)
                    .orElseThrow(() -> new ResourceNotFoundException("Message with ID " + entityId + " not found."));
            chat = message.getChat();
            if (chat == null) {
                throw new AccessDeniedException(secureChatAccess.message() + " Message not associated with a chat.");
            }

        } else {
            throw new IllegalStateException("Access control aspect misconfiguration: Cannot determine entity type (expected chatId or messageId).");
        }

        Integer chatCompanyId = chat.getCompany() != null ? chat.getCompany().getId() : null;

        boolean isManager = userRoles.contains(Role.MANAGER);
        boolean isOperator = userRoles.contains(Role.OPERATOR);
        boolean accessGranted = false;

        if (!isManager && !isOperator) {
            //TODO пока что тестовый чат создается как вк, поэтому вк
            if (chat.getChatChannel() == ChatChannel.VK) {
                accessGranted = true;
            } else {
                log.warn("Non-Operator/Manager user ID {} attempted to access non-test chat ID {}", currentUserId, chat.getId());
            }
        } else {
            if (chatCompanyId == null || !Objects.equals(userCompanyId, chatCompanyId)) {
                throw new AccessDeniedException(secureChatAccess.message() + " Chat belongs to a different company.");
            }

            if (isManager) {
                accessGranted = true;
            } else if (isOperator) {
                if (chat.getUser() != null && Objects.equals(chat.getUser().getId(), currentUserId)) {
                    accessGranted = true;
                } else {
                    log.warn("Operator ID {} attempted to access chat ID {} (not assigned to this chat)", currentUserId, chat.getId());
                }
            }
        }

        if (!accessGranted) {
            throw new AccessDeniedException(secureChatAccess.message());
        }
    }
}

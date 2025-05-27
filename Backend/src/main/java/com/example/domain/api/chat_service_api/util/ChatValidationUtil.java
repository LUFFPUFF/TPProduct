package com.example.domain.api.chat_service_api.util;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.domain.api.chat_service_api.exception_handler.exception.service.ChatServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.AccessDeniedException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ChatValidationUtil {

    public void ensureChatBelongsToCompany(Chat chat, Integer expectedCompanyId, String operationContext) throws AccessDeniedException {
        if (chat.getCompany() == null || !Objects.equals(chat.getCompany().getId(), expectedCompanyId)) {
            log.warn("[{}] Access Denied: Chat {} (company {}) does not belong to the expected company {}.",
                    operationContext, chat.getId(), chat.getCompany() != null ? chat.getCompany().getId() : "null", expectedCompanyId);
            throw new AccessDeniedException("Access Denied: Chat belongs to a different company.");
        }
    }

    public void ensureUserBelongsToCompany(User user, Integer expectedCompanyId, String errorMessage) throws AccessDeniedException {
        if (user.getCompany() == null || !Objects.equals(user.getCompany().getId(), expectedCompanyId)) {
            log.warn("Access Denied for user {}: User company {} does not match expected company {}. Context: {}",
                    user.getId(), user.getCompany() != null ? user.getCompany().getId() : "null", expectedCompanyId, errorMessage);
            throw new AccessDeniedException(errorMessage);
        }
    }

    public void ensureEntitiesBelongToSameCompany(String errorPrefix, Company... companies) throws ChatServiceException {
        if (companies == null || companies.length < 2) return;

        Set<Integer> companyIds = Arrays.stream(companies)
                .filter(Objects::nonNull)
                .map(Company::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (companyIds.size() > 1) {
            String details = Arrays.stream(companies)
                    .map(c -> c == null ? "null_company" : "Company(id=" + (c.getId() == null ? "null" : c.getId()) + ")")
                    .collect(Collectors.joining(", "));
            log.error("{} Entities belong to different companies: {}", errorPrefix, details);
            throw new ChatServiceException(errorPrefix + "Entities must belong to the same company.");
        }

        boolean anyNonNullCompanyHasNullId = Arrays.stream(companies)
                .anyMatch(c -> c != null && c.getId() == null);
        boolean anyEntityHasNullCompany = Arrays.stream(companies)
                .anyMatch(Objects::isNull);

        if (anyEntityHasNullCompany && !companyIds.isEmpty()){
            log.error("{} One or more entities are not associated with a company, while others are. Details: {}", errorPrefix, Arrays.stream(companies).map(c -> c == null ? "null_company" : "Company(id=" + (c.getId() == null ? "null" : c.getId()) + ")").collect(Collectors.joining(", ")));
            throw new ChatServiceException(errorPrefix + "One or more entities are not properly associated with a company.");
        }
        if (anyNonNullCompanyHasNullId) {
            log.error("{} One or more companies have a null ID. Details: {}", errorPrefix, Arrays.stream(companies).map(c -> "Company(id=" + (c.getId() == null ? "null" : c.getId()) + ")").collect(Collectors.joining(", ")));
            throw new ChatServiceException(errorPrefix + "One or more companies have an invalid ID.");
        }
    }

    public void ensureChatIsInStatus(Chat chat, ChatStatus expectedStatus, String errorMessage) throws ChatServiceException {
        if (chat.getStatus() != expectedStatus) {
            log.warn("Chat {} is in status {} but expected {}. Operation: {}", chat.getId(), chat.getStatus(), expectedStatus, errorMessage);
            throw new ChatServiceException(errorMessage + " Current status: " + chat.getStatus());
        }
    }

    public void ensureChatIsInStatus(Chat chat, Set<ChatStatus> expectedStatuses, String errorMessage) throws ChatServiceException {
        if (!expectedStatuses.contains(chat.getStatus())) {
            log.warn("Chat {} is in status {} but expected one of {}. Operation: {}", chat.getId(), chat.getStatus(), expectedStatuses, errorMessage);
            throw new ChatServiceException(errorMessage + " Current status: " + chat.getStatus());
        }
    }

    public void ensureClientOwnsChat(Chat chat, Integer clientId, String operationContext) throws ChatServiceException {
        if (chat.getClient() == null || !Objects.equals(chat.getClient().getId(), clientId)) {
            log.warn("[{}] Client validation failed for chat {}: Expected client {}, found {}.",
                    operationContext, chat.getId(), clientId, chat.getClient() != null ? chat.getClient().getId() : "null");
            throw new ChatServiceException("Client validation failed: You are not the client of this chat.");
        }
    }

    public void ensureChatNotClosedOrArchived(Chat chat, String errorMessage) throws ChatServiceException {
        if (chat.getStatus() == ChatStatus.CLOSED || chat.getStatus() == ChatStatus.ARCHIVED) {
            log.warn("Operation on chat {} failed: Chat is {} (closed or archived). Context: {}", chat.getId(), chat.getStatus(), errorMessage);
            throw new ChatServiceException(errorMessage + " Chat is already " + chat.getStatus() + ".");
        }
    }
}

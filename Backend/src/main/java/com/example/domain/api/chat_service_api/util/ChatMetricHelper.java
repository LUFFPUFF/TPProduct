package com.example.domain.api.chat_service_api.util;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.domain.api.statistics_module.metrics.service.IChatMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class ChatMetricHelper {

    private final IChatMetricsService chatMetricsService;

    public String getCompanyIdStr(Company company) {
        return (company != null && company.getId() != null) ? company.getId().toString() : "unknown";
    }

    public void incrementChatOperationError(String operationName, Company companyContext, String errorType) {
        chatMetricsService.incrementChatOperationError(operationName, getCompanyIdStr(companyContext), errorType);
    }

    public void incrementChatOperationError(String operationName, String companyIdIfKnown, String errorType) {
        chatMetricsService.incrementChatOperationError(operationName, companyIdIfKnown != null ? companyIdIfKnown : "unknown", errorType);
    }

    public void recordChatAssignmentTimeIfApplicable(Chat chat) {
        if (chat == null) {
            return;
        }
        if (chat.getCompany() != null &&
                chat.getChatChannel() != null &&
                chat.getAssignedAt() != null &&
                chat.getCreatedAt() != null) {
            if (chat.getAssignedAt().isBefore(chat.getCreatedAt())) {
                return;
            }

            Duration assignmentDuration = Duration.between(chat.getCreatedAt(), chat.getAssignedAt());


            chatMetricsService.recordChatAssignmentTime(
                    getCompanyIdStr(chat.getCompany()),
                    chat.getChatChannel(),
                    assignmentDuration
            );
        }
    }

    public void recordChatDuration(Chat chat) {
        if (chat == null) {
            return;
        }
        if (chat.getCompany() != null &&
                chat.getChatChannel() != null &&
                chat.getStatus() == ChatStatus.CLOSED &&
                chat.getClosedAt() != null &&
                chat.getCreatedAt() != null) {

            if (chat.getClosedAt().isBefore(chat.getCreatedAt())) {
                return;
            }

            Duration chatDuration = Duration.between(chat.getCreatedAt(), chat.getClosedAt());

            chatMetricsService.recordChatDuration(
                    getCompanyIdStr(chat.getCompany()),
                    chat.getChatChannel(),
                    ChatStatus.CLOSED,
                    chatDuration
            );
        }
    }

    public void incrementChatOperatorLinked(Company company, ChatChannel channel) {
        if (company == null || channel == null) {
            return;
        }

        chatMetricsService.incrementChatOperatorLinked(
                getCompanyIdStr(company),
                channel
        );
    }

    public void recordChatAssignmentTime(Chat chat, Duration assignmentDuration) {
        if (chat.getCompany() != null && chat.getChatChannel() != null && chat.getAssignedAt() != null && chat.getCreatedAt() != null) {
            Duration effectiveDuration = assignmentDuration.isZero() ?
                    Duration.between(chat.getCreatedAt(), chat.getAssignedAt()) : assignmentDuration;

            chatMetricsService.recordChatAssignmentTime(
                    getCompanyIdStr(chat.getCompany()),
                    chat.getChatChannel(),
                    effectiveDuration
            );
        }
    }
}

package com.example.domain.api.chat_service_api.service.impl.message;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.domain.api.chat_service_api.service.message.IOperatorChatInteractionService;
import com.example.domain.api.chat_service_api.util.ChatMetricHelper;
import com.example.domain.api.chat_service_api.util.MdcUtil;
import com.example.domain.api.statistics_module.metrics.service.IChatMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperatorChatInteractionServiceImpl implements IOperatorChatInteractionService {

    private final ChatRepository chatRepository;
    private final IChatMetricsService chatMetricsService;
    private final ChatMetricHelper chatMetricHelper;

    private static final String OPERATION_PROCESS_OPERATOR_MSG_IMPACT = "processOperatorMessageImpact";
    private static final String KEY_CHAT_ID = "chatId";
    private static final String KEY_OPERATOR_ID = "operatorId";
    private static final String KEY_MESSAGE_ID = "messageId";
    private static final String KEY_COMPANY_ID_MDC = "companyIdMdc";

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public boolean processOperatorMessageImpact(Chat chat, User operator, ChatMessage operatorMessage) {
        return MdcUtil.withContext(
                () -> {
                    log.info("Processing operator message impact for chat {}, operator {}, message {}",
                            chat.getId(), operator.getId(), operatorMessage.getId());
                    MDC.put(KEY_COMPANY_ID_MDC, chatMetricHelper.getCompanyIdStr(chat.getCompany()));

                    boolean chatStateChanged = false;
                    LocalDateTime now = LocalDateTime.now();

                    if (chat.getUser() == null || !Objects.equals(chat.getUser().getId(), operator.getId())) {
                        log.info("Chat {} operator is being set/changed to {} from {}. Current assignedAt: {}",
                                chat.getId(), operator.getId(),
                                (chat.getUser() != null ? chat.getUser().getId() : "none"),
                                chat.getAssignedAt());
                        chat.setUser(operator);

                        if (chat.getAssignedAt() == null || chat.getStatus() == ChatStatus.PENDING_OPERATOR) {
                            chat.setAssignedAt(now);
                        }
                        chatStateChanged = true;
                    } else if (chat.getAssignedAt() == null && (chat.getStatus() == ChatStatus.ASSIGNED || chat.getStatus() == ChatStatus.PENDING_OPERATOR)) {
                        chat.setAssignedAt(now);

                    }

                    if (chat.getStatus() == ChatStatus.ASSIGNED || chat.getStatus() == ChatStatus.PENDING_OPERATOR) {
                        chat.setStatus(ChatStatus.IN_PROGRESS);
                        chatStateChanged = true;
                    }

                    if (Boolean.FALSE.equals(chat.getHasOperatorResponded()) || chat.getHasOperatorResponded() == null) {
                        if (chat.getAssignedAt() != null) {

                            if (!operatorMessage.getSentAt().isBefore(chat.getAssignedAt())) {
                                Duration firstResponseTime = Duration.between(chat.getAssignedAt(), operatorMessage.getSentAt());

                                chatMetricsService.recordChatFirstOperatorResponseTime(
                                        chatMetricHelper.getCompanyIdStr(chat.getCompany()),
                                        chat.getChatChannel() != null ? chat.getChatChannel() : ChatChannel.UNKNOWN,
                                        firstResponseTime
                                );
                                chat.setHasOperatorResponded(true);
                                chatStateChanged = true;
                                log.info("First operator response registered for chat {} by operator {}. Response time: {}",
                                        chat.getId(), operator.getId(), firstResponseTime);
                            } else {
                                log.warn("Operator message (ID: {}) sentAt ({}) is before chat (ID: {}) assignedAt ({}). " +
                                                "First response time logic potentially affected or skipped for this interaction.",
                                        operatorMessage.getId(), operatorMessage.getSentAt(),
                                        chat.getId(), chat.getAssignedAt());
                            }
                        } else {
                            log.warn("Cannot calculate first operator response time for chat {}: assignedAt is null, " +
                                            "even though operator {} sent a message. Status: {}",
                                    chat.getId(), operator.getId(), chat.getStatus());

                            chat.setHasOperatorResponded(true);
                            chatStateChanged = true;
                        }
                    }

                    if (chatStateChanged || chat.getUser() == operator) {
                        chatRepository.save(chat);
                    }

                    return chatStateChanged;
                },
                "operation", OPERATION_PROCESS_OPERATOR_MSG_IMPACT,
                KEY_CHAT_ID, chat.getId(),
                KEY_OPERATOR_ID, operator.getId(),
                KEY_MESSAGE_ID, operatorMessage.getId()
        );
    }
}

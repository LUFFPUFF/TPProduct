package com.example.domain.api.chat_service_api.service.impl.message;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.database.model.chats_messages_module.message.MessageStatus;
import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.repository.chats_messages_module.ChatMessageRepository;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.domain.api.chat_service_api.event.message.ChatMessageStatusUpdatedEvent;
import com.example.domain.api.chat_service_api.exception_handler.ChatNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.exception.service.ChatServiceException;
import com.example.domain.api.chat_service_api.mapper.ChatMessageMapper;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.api.chat_service_api.service.message.IChatMessageStatusService;
import com.example.domain.api.chat_service_api.util.ChatMetricHelper;
import com.example.domain.api.chat_service_api.util.ChatValidationUtil;
import com.example.domain.api.chat_service_api.util.MdcUtil;
import com.example.domain.api.statistics_module.metrics.service.IChatMetricsService;
import com.example.domain.security.model.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.nio.file.AccessDeniedException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageStatusServiceImpl implements IChatMessageStatusService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRepository chatRepository;
    private final ChatMessageMapper chatMessageMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final IChatMetricsService chatMetricsService;
    private final ChatMetricHelper chatMetricHelper;
    private final ChatValidationUtil chatValidationUtil;

    private static final String OPERATION_UPDATE_MSG_STATUS = "updateMessageStatus";
    private static final String OPERATION_MARK_AS_READ = "markClientMessagesAsRead";
    private static final String OPERATION_UPDATE_MSG_STATUS_EXT = "updateOperatorMessageStatusByExternalId";

    private static final String KEY_MESSAGE_ID = "messageId";
    private static final String KEY_CHAT_ID = "chatId";
    private static final String KEY_NEW_STATUS = "newStatus";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_OPERATOR_ID = "operatorId";
    private static final String KEY_EXTERNAL_MSG_ID = "externalMessageId";
    private static final String KEY_COMPANY_ID_MDC = "companyIdMdc";

    @Override
    @Transactional
    public MessageDto updateMessageStatus(Integer messageId, MessageStatus newStatus, UserContext userContext) throws AccessDeniedException {
        return MdcUtil.withContext(
                () -> {
                    log.info("User {} attempting to update status of message ID {} to {}",
                            userContext != null ? userContext.getUserId() : "System", messageId, newStatus);

                    ChatMessage message = chatMessageRepository.findById(messageId)
                            .orElseThrow(() -> new ResourceNotFoundException("Message with ID " + messageId + " not found."));

                    Chat chat = message.getChat();
                    if (chat == null) {
                        log.error("Message {} is not associated with any chat.", messageId);
                        throw new ChatServiceException("Message " + messageId + " has no associated chat.");
                    }
                    MDC.put(KEY_COMPANY_ID_MDC, chatMetricHelper.getCompanyIdStr(chat.getCompany()));

                    if (userContext != null) {
                        chatValidationUtil.ensureChatBelongsToCompany(chat, userContext.getCompanyId(), OPERATION_UPDATE_MSG_STATUS);
                    }

                    if (message.getStatus() == newStatus) {
                        return chatMessageMapper.toDto(message);
                    }

                    message.setStatus(newStatus);
                    ChatMessage updatedMessage = chatMessageRepository.save(message);

                    recordAndUpdateStatusMetrics(updatedMessage);
                    publishStatusUpdateEvent(updatedMessage);

                    return chatMessageMapper.toDto(updatedMessage);
                },
                "operation", OPERATION_UPDATE_MSG_STATUS,
                KEY_MESSAGE_ID, messageId,
                KEY_NEW_STATUS, newStatus.name(),
                KEY_USER_ID, userContext != null ? userContext.getUserId() : "System"
        );
    }

    @Override
    @Transactional
    public int markClientMessagesAsRead(Integer chatId, Integer operatorId, Collection<Integer> messageIds, UserContext userContext) throws AccessDeniedException {
        return MdcUtil.withContext(
                () -> {
                    log.info("Operator {} (UserContext: {}) attempting to mark messages {} as read in chat {}",
                            operatorId, userContext.getUserId(), messageIds, chatId);

                    if (CollectionUtils.isEmpty(messageIds)) {
                        return 0;
                    }

                    Chat chat = chatRepository.findById(chatId)
                            .orElseThrow(() -> new ChatNotFoundException("Chat with ID " + chatId + " not found."));
                    MDC.put(KEY_COMPANY_ID_MDC, chatMetricHelper.getCompanyIdStr(chat.getCompany()));


                    chatValidationUtil.ensureChatBelongsToCompany(chat, userContext.getCompanyId(), OPERATION_MARK_AS_READ);
                    if (!Objects.equals(userContext.getUserId(), operatorId) && !userContext.getRoles().contains(Role.MANAGER)) {
                        log.warn("User {} attempted to mark messages as read on behalf of operator {} without manager role in chat {}.",
                                userContext.getUserId(), operatorId, chatId);
                        throw new AccessDeniedException("You can only mark messages as read for yourself or act as a manager.");
                    }

                    if (chat.getUser() == null || (!Objects.equals(chat.getUser().getId(), operatorId) && !userContext.getRoles().contains(Role.MANAGER))) {
                        log.warn("Operator {} attempted to mark messages as read in chat {} where they are not assigned and are not a manager. Assigned: {}",
                                operatorId, chatId, chat.getUser() != null ? chat.getUser().getId() : "none");
                        throw new AccessDeniedException("Only the assigned operator or a manager can mark messages as read in this chat.");
                    }


                    List<ChatMessage> messagesToUpdate = chatMessageRepository.findAllByIdInAndChatIdAndSenderTypeAndStatusNot(
                            messageIds, chatId, ChatMessageSenderType.CLIENT, MessageStatus.READ
                    );

                    if (messagesToUpdate.isEmpty()) {
                        log.debug("No client messages found matching criteria for chat {} to mark as read.", chatId);
                        return 0;
                    }

                    int updatedCount = 0;
                    List<ChatMessage> successfullyUpdatedMessages = new ArrayList<>();
                    for (ChatMessage message : messagesToUpdate) {
                        if (!Objects.equals(message.getChat().getId(), chatId) ||
                                message.getSenderType() != ChatMessageSenderType.CLIENT ||
                                message.getStatus() == MessageStatus.READ) {
                            continue;
                        }
                        message.setStatus(MessageStatus.READ);
                        successfullyUpdatedMessages.add(message);
                        updatedCount++;
                    }

                    if (!successfullyUpdatedMessages.isEmpty()) {
                        chatMessageRepository.saveAll(successfullyUpdatedMessages);
                        successfullyUpdatedMessages.forEach(updatedMessage -> {
                            recordAndUpdateStatusMetrics(updatedMessage);
                            publishStatusUpdateEvent(updatedMessage);
                        });
                    }
                    return updatedCount;
                },
                "operation", OPERATION_MARK_AS_READ,
                KEY_CHAT_ID, chatId,
                KEY_OPERATOR_ID, operatorId,
                KEY_USER_ID, userContext.getUserId()
        );
    }

    @Override
    @Transactional
    public int updateOperatorMessageStatusByExternalId(Integer chatId, String externalMessageId, MessageStatus newStatus) {
        return MdcUtil.withContext(
                () -> {
                    log.info("Updating operator message status by external ID {} in chat {} to {}",
                            externalMessageId, chatId, newStatus);

                    Optional<ChatMessage> messageOptional = chatMessageRepository.findByChatIdAndExternalMessageIdAndSenderType(
                            chatId, externalMessageId, ChatMessageSenderType.OPERATOR
                    );

                    if (messageOptional.isEmpty()) {
                        log.warn("Operator message with external ID {} not found in chat {} for status update.",
                                externalMessageId, chatId);
                        chatMetricHelper.incrementChatOperationError(OPERATION_UPDATE_MSG_STATUS_EXT, "unknown_company_by_ext_id", "MessageNotFoundByExternalId");
                        return 0;
                    }

                    ChatMessage message = messageOptional.get();

                    Chat chatForMetrics = message.getChat();
                    if (chatForMetrics != null) {
                        MDC.put(KEY_COMPANY_ID_MDC, chatMetricHelper.getCompanyIdStr(chatForMetrics.getCompany()));
                    }


                    if (message.getStatus() == newStatus) {
                        return 0;
                    }

                    message.setStatus(newStatus);
                    ChatMessage updatedMessage = chatMessageRepository.save(message);

                    recordAndUpdateStatusMetrics(updatedMessage);
                    publishStatusUpdateEvent(updatedMessage);

                    return 1;
                },
                "operation", OPERATION_UPDATE_MSG_STATUS_EXT,
                KEY_CHAT_ID, chatId,
                KEY_EXTERNAL_MSG_ID, externalMessageId,
                KEY_NEW_STATUS, newStatus.name()
        );
    }

    private void recordAndUpdateStatusMetrics(ChatMessage updatedMessage) {
        Chat chat = updatedMessage.getChat();
        String companyIdStr = "unknown";
        ChatChannel channel = ChatChannel.UNKNOWN;

        if (chat != null) {
            companyIdStr = chatMetricHelper.getCompanyIdStr(chat.getCompany());
            channel = chat.getChatChannel() != null ? chat.getChatChannel() : ChatChannel.UNKNOWN;
        } else {
            log.warn("Cannot record message status metrics for message {} as it has no associated chat.", updatedMessage.getId());
        }

        chatMetricsService.incrementMessageStatusUpdated(
                companyIdStr,
                channel,
                updatedMessage.getStatus().name()
        );
    }

    private void publishStatusUpdateEvent(ChatMessage updatedMessage) {
        MessageDto updatedMessageDTO = chatMessageMapper.toDto(updatedMessage);
        eventPublisher.publishEvent(new ChatMessageStatusUpdatedEvent(this, updatedMessageDTO));
        log.debug("Published ChatMessageStatusUpdatedEvent for message ID {} to status {}",
                updatedMessage.getId(), updatedMessage.getStatus());
    }
}

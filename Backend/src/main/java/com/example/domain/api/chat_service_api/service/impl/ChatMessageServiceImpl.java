package com.example.domain.api.chat_service_api.service.impl;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.database.model.chats_messages_module.message.MessageStatus;
import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.crm_module.client.Client;
import com.example.database.repository.chats_messages_module.ChatMessageRepository;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.database.repository.crm_module.ClientRepository;
import com.example.domain.api.chat_service_api.exception_handler.ChatNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.exception.ExternalMessagingException;
import com.example.domain.api.chat_service_api.exception_handler.exception.service.ChatServiceException;
import com.example.domain.api.chat_service_api.integration.service.IExternalMessagingService;
import com.example.domain.api.chat_service_api.mapper.ChatMessageMapper;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.api.chat_service_api.model.dto.MessageStatusUpdateDTO;
import com.example.domain.api.chat_service_api.model.rest.mesage.SendMessageRequestDTO;
import com.example.domain.api.chat_service_api.service.IChatMessageService;
import com.example.domain.api.chat_service_api.service.WebSocketMessagingService;

import com.example.domain.api.statistics_module.metrics.service.IChatMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.example.domain.api.chat_service_api.service.impl.LeastBusyAssignmentService.OPEN_CHAT_STATUSES;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageServiceImpl implements IChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final ChatMessageMapper chatMessageMapper;
    private final WebSocketMessagingService messagingService;
    private final IExternalMessagingService externalMessagingService;
    private final IChatMetricsService chatMetricsService;

    private static final int MAX_CONTENT_LENGTH = 255;
    private static final String TRUNCATE_INDICATOR = "...";

    @Override
    @Transactional
    public MessageDto processAndSaveMessage(SendMessageRequestDTO messageRequest, Integer senderId, ChatMessageSenderType senderType) {
        Chat chat = chatRepository.findById(messageRequest.getChatId())
                .orElseThrow(() -> new ChatNotFoundException("Chat with ID " + messageRequest.getChatId() + " not found"));

        String companyIdStr = (chat.getCompany() != null && chat.getCompany().getId() != null) ? chat.getCompany().getId().toString() : "unknown";
        ChatChannel channel = chat.getChatChannel() != null ? chat.getChatChannel() : ChatChannel.UNKNOWN;

        User senderOperator = null;
        Client senderClient = null;

        if (senderType == ChatMessageSenderType.OPERATOR) {
            senderOperator = userRepository.findById(senderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Operator with ID " + senderId + " not found."));
            checkAndUpdateChatStatus(chat, senderOperator);
        } else if (senderType == ChatMessageSenderType.CLIENT) {
            senderClient = clientRepository.findById(senderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client with ID " + senderId + " not found."));
        } else if (senderType == ChatMessageSenderType.AUTO_RESPONDER) {
            senderClient = clientRepository.findById(senderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client with ID " + senderId + " not found."));
        } else {
            throw new IllegalArgumentException("Unsupported sender type: " + senderType);
        }

        ChatMessage message = new ChatMessage();
        message.setChat(chat);
        message.setSenderOperator(senderOperator);
        message.setSenderClient(senderClient);
        message.setSenderType(senderType);
        message.setContent(truncateContent(messageRequest.getContent(), MAX_CONTENT_LENGTH, TRUNCATE_INDICATOR));
        message.setExternalMessageId(messageRequest.getExternalMessageId());
        message.setReplyToExternalMessageId(messageRequest.getReplyToExternalMessageId());
        message.setSentAt(LocalDateTime.now());
        message.setStatus(MessageStatus.SENT);

        // TODO: Обработка вложений messageRequest.getAttachments()

        try {
            ChatMessage savedMessage = chatMessageRepository.save(message);

            chatMetricsService.incrementMessagesSent(
                    companyIdStr,
                    channel,
                    senderType
            );

            if (savedMessage.getContent() != null) {
                chatMetricsService.recordMessageContentLength(
                        companyIdStr,
                        channel,
                        senderType,
                        savedMessage.getContent().length()
                );
            }

            chat.setLastMessageAt(savedMessage.getSentAt());
            chatRepository.save(chat);

            MessageDto savedMessageDTO = chatMessageMapper.toDto(savedMessage);

            String destination = "/topic/chat/" + chat.getId() + "/messages";
            messagingService.sendMessage(destination, savedMessageDTO);

            if (senderType == ChatMessageSenderType.OPERATOR && savedMessage.getContent() != null && !savedMessage.getContent().trim().isEmpty()) {
                log.info("Processing message from OPERATOR for external sending to chat ID {}", chat.getId());
                try {
                    externalMessagingService.sendMessageToExternal(chat.getId(), savedMessage.getContent());
                    log.info("Operator message for chat ID {} successfully placed for external sending.", chat.getId());
                } catch (ExternalMessagingException e) {
                    log.error("Failed to place operator message for chat ID {} into external sending queue.", chat.getId(), e);
                }
            }
            return savedMessageDTO;
        } catch (ChatNotFoundException | ResourceNotFoundException | IllegalArgumentException e) {
            chatMetricsService.incrementChatOperationError("processAndSaveMessage", companyIdStr, e.getClass().getSimpleName());
            throw e;
        } catch (Exception e) {
            chatMetricsService.incrementChatOperationError("processAndSaveMessage", companyIdStr, "UnexpectedException");
            throw new ChatServiceException("An unexpected error occurred.", e);
        }
    }

    @Override
    @Transactional(readOnly = true)

    public List<MessageDto> getMessagesByChatId(Integer chatId) {
        List<ChatMessage> messages = chatMessageRepository.findByChatIdOrderBySentAtAsc(chatId);

        return messages.stream()
                .map(chatMessageMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional

    public MessageDto updateMessageStatus(Integer messageId, MessageStatus newStatus) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message with ID " + messageId + " not found."));

        Chat chat = message.getChat();
        String companyIdStr = (chat.getCompany() != null && chat.getCompany().getId() != null) ? chat.getCompany().getId().toString() : "unknown";
        ChatChannel channel = chat.getChatChannel() != null ? chat.getChatChannel() : ChatChannel.UNKNOWN;

        try {
            message.setStatus(newStatus);
            ChatMessage updatedMessage = chatMessageRepository.save(message);

            chatMetricsService.incrementMessageStatusUpdated(
                    companyIdStr,
                    channel,
                    newStatus.name()
            );

            MessageDto updatedMessageDTO = chatMessageMapper.toDto(updatedMessage);

            MessageStatusUpdateDTO statusUpdateDTO = new MessageStatusUpdateDTO();
            statusUpdateDTO.setMessageId(updatedMessage.getId());
            statusUpdateDTO.setChatId(updatedMessage.getChat().getId());
            statusUpdateDTO.setNewStatus(updatedMessage.getStatus());
            statusUpdateDTO.setTimestamp(LocalDateTime.now());

            String destination = "/topic/chat/" + updatedMessage.getChat().getId() + "/messages";
            messagingService.sendMessage(destination, statusUpdateDTO);

            return updatedMessageDTO;
        } catch (ResourceNotFoundException e) {
            chatMetricsService.incrementChatOperationError("updateMessageStatus", companyIdStr, e.getClass().getSimpleName());
            throw e;
        } catch (Exception e) {
            chatMetricsService.incrementChatOperationError("updateMessageStatus", companyIdStr, "UnexpectedException");
            throw new ChatServiceException("An unexpected error occurred.", e);
        }
    }

    @Override
    @Transactional

    public int markClientMessagesAsRead(Integer chatId, Integer operatorId, Collection<Integer> messageIds) {
        Chat chat = chatRepository.findById(chatId).orElse(null);
        if (chat == null) {
            chatMetricsService.incrementChatOperationError("markClientMessagesAsRead", "unknown", "ChatNotFound");
            return 0;
        }
        String companyIdStr = (chat.getCompany() != null && chat.getCompany().getId() != null) ? chat.getCompany().getId().toString() : "unknown";
        ChatChannel channel = chat.getChatChannel() != null ? chat.getChatChannel() : ChatChannel.UNKNOWN;

        if (messageIds == null || messageIds.isEmpty()) {
            return 0;
        }

        try {
            int updatedCount = chatMessageRepository.markClientMessagesAsRead(chatId, MessageStatus.READ, messageIds);

            if (updatedCount > 0) {
                String destination = "/topic/chat/" + chatId + "/messages";
                for (Integer messageId : messageIds) {
                    MessageStatusUpdateDTO statusUpdateDTO = new MessageStatusUpdateDTO();
                    statusUpdateDTO.setMessageId(messageId);
                    statusUpdateDTO.setChatId(chatId);
                    statusUpdateDTO.setNewStatus(MessageStatus.READ);
                    statusUpdateDTO.setTimestamp(LocalDateTime.now());
                    messagingService.sendMessage(destination, statusUpdateDTO);
                    chatMetricsService.incrementMessagesReadByOperator(companyIdStr, channel);
                }
            }
            return updatedCount;
        } catch (Exception e) {
            chatMetricsService.incrementChatOperationError("markClientMessagesAsRead", companyIdStr, e.getClass().getSimpleName());
            throw new ChatServiceException("Failed to mark messages as read", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Chat> findOpenChatByClientAndChannel(Integer clientId, ChatChannel channel) {
        Optional<Chat> foundChat = chatMessageRepository
                .findFirstChatByChatClient_IdAndChatChannelAndChatStatusInOrderByChatCreatedAtDesc(clientId, channel, OPEN_CHAT_STATUSES);
        if(foundChat.isPresent()) {
            log.debug("Found open chat ID {} via ChatMessageService for client {} on channel {}", foundChat.get().getId(), clientId, channel);
        } else {
            log.debug("No open chat found via ChatMessageService for client {} on channel {}", clientId, channel);
        }
        return foundChat;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChatMessage> findFirstMessageByChatId(Integer chatId) {
        Optional<ChatMessage> firstMessage = chatMessageRepository.findFirstByChatIdOrderBySentAtAsc(chatId);
        if (firstMessage.isPresent()) {
            log.debug("Found first message ID {} for chat ID {}", firstMessage.get().getId(), chatId);
        } else {
            log.debug("No messages found for chat ID {}", chatId);
        }
        return firstMessage;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Chat> findChatEntityById(Integer chatId) {
        return chatRepository.findById(chatId);
    }

    @Override

    public int updateOperatorMessageStatusByExternalId(Integer chatId, String externalMessageId, MessageStatus newStatus) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Chat with ID " + chatId + " not found"));

        int updatedCount = chatMessageRepository.updateOperatorMessageStatusByExternalId(chatId, newStatus, externalMessageId);

        if (updatedCount > 0) {
            Optional<ChatMessage> updatedMsgOptional = chatMessageRepository.findByExternalMessageId(externalMessageId)
                    .filter(msg -> Objects.equals(msg.getChat().getId(), chatId));

            if (updatedMsgOptional.isPresent()) {
                ChatMessage updatedMsg = updatedMsgOptional.get();
                MessageStatusUpdateDTO statusUpdateDTO = new MessageStatusUpdateDTO();
                statusUpdateDTO.setMessageId(updatedMsg.getId());
                statusUpdateDTO.setChatId(updatedMsg.getChat().getId());
                statusUpdateDTO.setNewStatus(newStatus);
                statusUpdateDTO.setTimestamp(LocalDateTime.now());

                String destination = "/topic/chat/" + chat.getId() + "/messages";
                messagingService.sendMessage(destination, statusUpdateDTO);
            } else {
                log.warn("Warning: Updated status for external message ID {} in chat {}, but could not retrieve updated message for WS notification.", externalMessageId, chatId);
            }
        }

        return updatedCount;
    }

    @Override
    @Deprecated
    public ChatMessage saveIncomingMessageFromExternal(Integer chatId, ChatMessageSenderType senderType, Integer senderId, String content, String externalMessageId, String replyToExternalMessageId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void checkAndUpdateChatStatus(Chat chat, User operator) {
        if (chat.getStatus() != ChatStatus.ASSIGNED) {
            return;
        }

        long operatorMessagesCount = chatMessageRepository.countByChatAndSenderOperatorAndSenderType(
                chat,
                operator,
                ChatMessageSenderType.OPERATOR
        );

        if (operatorMessagesCount >= 5) {
            chat.setStatus(ChatStatus.IN_PROGRESS);
            chatRepository.save(chat);

            log.info("Chat {} status updated to IN_PROGRESS (operator {} sent {} messages)",
                    chat.getId(), operator.getId(), operatorMessagesCount);
        }
    }

    private String truncateContent(String content, int maxLength, String truncateIndicator) {
        if (content == null) {
            return null;
        }
        if (content.length() <= maxLength) {
            return content;
        }
        if (maxLength <= truncateIndicator.length()) {
            return "";
        }
        String truncated = content.substring(0, maxLength - truncateIndicator.length());
        return truncated + truncateIndicator;
    }
}

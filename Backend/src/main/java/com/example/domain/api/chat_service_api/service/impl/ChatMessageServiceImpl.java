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
import com.example.domain.api.chat_service_api.event.chat.ChatStatusChangedEvent;
import com.example.domain.api.chat_service_api.event.message.ChatMessageSentEvent;
import com.example.domain.api.chat_service_api.event.message.ChatMessageStatusUpdatedEvent;
import com.example.domain.api.chat_service_api.exception_handler.ChatNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.exception.ExternalMessagingException;
import com.example.domain.api.chat_service_api.integration.service.IExternalMessagingService;
import com.example.domain.api.chat_service_api.mapper.ChatMapper;
import com.example.domain.api.chat_service_api.mapper.ChatMessageMapper;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.api.chat_service_api.model.rest.mesage.SendMessageRequestDTO;
import com.example.domain.api.chat_service_api.service.IChatMessageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final IExternalMessagingService externalMessagingService;
    private final ApplicationEventPublisher eventPublisher;
    private final ChatMapper chatMapper;

    private static final int MAX_CONTENT_LENGTH = 255;
    private static final String TRUNCATE_INDICATOR = "...";

    @Override
    @Transactional
    public MessageDto processAndSaveMessage(SendMessageRequestDTO messageRequest, Integer senderId, ChatMessageSenderType senderType) {
        Chat chat = chatRepository.findById(messageRequest.getChatId())
                .orElseThrow(() -> new ChatNotFoundException("Chat with ID " + messageRequest.getChatId() + " not found"));

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

        ChatMessage savedMessage = chatMessageRepository.save(message);

        chat.setLastMessageAt(savedMessage.getSentAt());
        chatRepository.save(chat);

        MessageDto savedMessageDTO = chatMessageMapper.toDto(savedMessage);

        eventPublisher.publishEvent(new ChatMessageSentEvent(this, savedMessageDTO));
        log.debug("Published ChatMessageSentEvent for message ID {}", savedMessage.getId());

        eventPublisher.publishEvent(new ChatStatusChangedEvent(this, chat.getId(), chatMapper.toDto(chat)));

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

        message.setStatus(newStatus);
        ChatMessage updatedMessage = chatMessageRepository.save(message);
        MessageDto updatedMessageDTO = chatMessageMapper.toDto(updatedMessage);

        eventPublisher.publishEvent(new ChatMessageStatusUpdatedEvent(this, updatedMessageDTO));
        log.debug("Published ChatMessageStatusUpdatedEvent for message ID {} to status {}", updatedMessage.getId(), newStatus);

        return updatedMessageDTO;
    }

    @Override
    @Transactional

    public int markClientMessagesAsRead(Integer chatId, Integer operatorId, Collection<Integer> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return 0;
        }

        List<ChatMessage> messagesToUpdate = chatMessageRepository.findAllById(messageIds).stream()
                .filter(msg -> Objects.equals(msg.getChat().getId(), chatId) &&
                        msg.getSenderType() == ChatMessageSenderType.CLIENT &&
                        msg.getStatus() != MessageStatus.READ)
                .toList();

        if (messagesToUpdate.isEmpty()) {
            return 0;
        }

        int updatedCount = 0;
        for (ChatMessage message : messagesToUpdate) {
            message.setStatus(MessageStatus.READ);
            chatMessageRepository.save(message);
            updatedCount++;

            MessageDto updatedMessageDTO = chatMessageMapper.toDto(message);
            eventPublisher.publishEvent(new ChatMessageStatusUpdatedEvent(this, updatedMessageDTO));
            log.debug("Published ChatMessageStatusUpdatedEvent for read client message ID {}", updatedMessageDTO.getId());
        }
        return updatedCount;
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

        Optional<ChatMessage> messageOptional = chatMessageRepository.findByIdAndExternalMessageId(chatId, externalMessageId);
        if (messageOptional.isEmpty()) {
            log.warn("Message with external ID {} not found in chat {} for status update by external ID.", externalMessageId, chatId);
            return 0;
        }

        ChatMessage message = messageOptional.get();
        if (message.getStatus() == newStatus) {
            log.debug("Operator message {} (external ID {}) in chat {} already has status {}. No event published.",
                    message.getId(), externalMessageId, chatId, newStatus);
            return 0;
        }

        message.setStatus(newStatus);
        ChatMessage updatedMessage = chatMessageRepository.save(message);

        MessageDto updatedMessageDTO = chatMessageMapper.toDto(updatedMessage);
        eventPublisher.publishEvent(new ChatMessageStatusUpdatedEvent(this, updatedMessageDTO));
        log.debug("Published ChatMessageStatusUpdatedEvent for operator message ID {} (external ID {}), new status {}",
                updatedMessage.getId(), externalMessageId, newStatus);

        return 1;
    }

    @Override
    @Deprecated
    public ChatMessage saveIncomingMessageFromExternal(Integer chatId, ChatMessageSenderType senderType, Integer senderId, String content, String externalMessageId, String replyToExternalMessageId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private boolean checkAndUpdateChatStatus(Chat chat, User operator) {
        boolean statusChanged = false;
        if (chat.getStatus() == ChatStatus.ASSIGNED) {
            if (chat.getUser() == null || Objects.equals(chat.getUser().getId(), operator.getId())) {
                if (chat.getUser() == null) {
                    chat.setUser(operator);
                    if (chat.getAssignedAt() == null) chat.setAssignedAt(LocalDateTime.now());
                }
                chat.setStatus(ChatStatus.IN_PROGRESS);
                statusChanged = true;
                log.info("Chat {} status updated to IN_PROGRESS by operator {}", chat.getId(), operator.getId());
            }
        }

        return statusChanged;
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

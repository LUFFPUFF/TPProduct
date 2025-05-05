package com.example.domain.api.chat_service_api.service.impl;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.database.model.chats_messages_module.message.MessageStatus;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.crm_module.client.Client;
import com.example.database.repository.chats_messages_module.ChatMessageRepository;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.ans_api_module.event.AutoResponderEscalationEvent;
import com.example.domain.api.ans_api_module.exception.AutoResponderException;
import com.example.domain.api.ans_api_module.service.IAutoResponderService;
import com.example.domain.api.chat_service_api.exception_handler.ChatNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.exception.service.ChatServiceException;
import com.example.domain.api.chat_service_api.mapper.ChatMapper;
import com.example.domain.api.chat_service_api.model.dto.ChatDTO;
import com.example.domain.api.chat_service_api.model.dto.ChatDetailsDTO;
import com.example.domain.api.chat_service_api.model.rest.chat.AssignChatRequestDTO;
import com.example.domain.api.chat_service_api.model.rest.chat.CloseChatRequestDTO;
import com.example.domain.api.chat_service_api.model.rest.chat.CreateChatRequestDTO;
import com.example.domain.api.chat_service_api.service.*;
import com.example.domain.api.chat_service_api.service.security.IChatSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.example.domain.api.chat_service_api.service.impl.LeastBusyAssignmentService.OPEN_CHAT_STATUSES;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements IChatService {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final IClientService clientService;
    private final IUserService userService;
    private final ChatMapper chatMapper;
    private final ChatMessageRepository chatMessageRepository;
    private final IAssignmentService assignmentService;
    private final WebSocketMessagingService messagingService;
    private final INotificationService notificationService;
    private final IAutoResponderService autoResponderService;

    @Override
    @Transactional
    public ChatDetailsDTO createChat(CreateChatRequestDTO createRequest) {

        Client client = clientService.findById(createRequest.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client with ID " + createRequest.getClientId() + " not found"));

        Company company = client.getCompany();
        if (company == null) {
            log.error("Client with ID {} is not associated with a company.", createRequest.getClientId());
            throw new ResourceNotFoundException("Client is not associated with a company.");
        }

        Optional<Chat> existingChat = findOpenChatByClientAndChannel(createRequest.getClientId(), createRequest.getChatChannel());
        if (existingChat.isPresent()) {
            log.warn("Open chat ID {} already exists for client {} on channel {}", existingChat.get().getId(), createRequest.getClientId(), createRequest.getChatChannel());
            throw new ChatServiceException("Open chat already exists for this client and channel.");
        }

        Chat chat = createChatEntity(client, company, createRequest.getChatChannel());
        log.debug("Created initial chat entity with ID: {}", chat.getId());

        Chat savedChat = chatRepository.save(chat);
        log.info("Saved initial chat entity with ID: {}", savedChat.getId());

        if (createRequest.getInitialMessageContent() != null && !createRequest.getInitialMessageContent().isBlank()) {
            ChatMessage firstMessage = createChatMessageEntity(savedChat, client, null,
                    ChatMessageSenderType.CLIENT, createRequest.getInitialMessageContent(), null, null);
            chatMessageRepository.save(firstMessage);
            savedChat.setLastMessageAt(firstMessage.getSentAt());
        } else {
            savedChat.setLastMessageAt(savedChat.getCreatedAt());
        }

        chatRepository.save(savedChat);

        try {
            autoResponderService.processNewPendingChat(savedChat.getId());
            log.info("Auto-responder triggered for chat ID: {}", savedChat.getId());
        } catch (AutoResponderException e) { log.error("Failed to trigger auto-responder for chat ID {}: {}", savedChat.getId(), e.getMessage(), e);
            throw new AutoResponderException(e.getMessage());
        }

        return chatMapper.toDetailsDto(savedChat);
    }

    @Override
    @Transactional
    public ChatDetailsDTO createChatWithOperator(CreateChatRequestDTO createRequest) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Optional<User> currentUserOpt = userRepository.findByEmail(authentication.getName());

        User currentUser = currentUserOpt
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Client client = clientService.findById(createRequest.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client with ID " + createRequest.getClientId() + " not found"));

        Company company = client.getCompany();
        if (company == null) {
            log.error("Client with ID {} is not associated with a company.", createRequest.getClientId());
            throw new ResourceNotFoundException("Client is not associated with a company.");
        }

        Optional<Chat> existingChat = findOpenChatByClientAndChannel(createRequest.getClientId(), createRequest.getChatChannel());
        if (existingChat.isPresent()) {
            log.warn("Open chat ID {} already exists for client {} on channel {}", existingChat.get().getId(), createRequest.getClientId(), createRequest.getChatChannel());
            throw new ChatServiceException("Open chat already exists for this client and channel.");
        }

        Chat chat = createChatEntity(client, company, createRequest.getChatChannel());
        chat.setUser(currentUser);
        log.debug("Created initial chat entity with ID: {}", chat.getId());

        Chat savedChat = chatRepository.save(chat);
        log.info("Saved initial chat entity with ID: {}", savedChat.getId());

        if (createRequest.getInitialMessageContent() != null && !createRequest.getInitialMessageContent().isBlank()) {
            ChatMessage firstMessage = createChatMessageEntity(savedChat, client, null,
                    ChatMessageSenderType.CLIENT, createRequest.getInitialMessageContent(), null, null);
            chatMessageRepository.save(firstMessage);
            savedChat.setLastMessageAt(firstMessage.getSentAt());
        } else {
            savedChat.setLastMessageAt(savedChat.getCreatedAt());
        }

        chatRepository.save(savedChat);

        try {
            autoResponderService.processNewPendingChat(savedChat.getId());
            log.info("Auto-responder triggered for chat ID: {}", savedChat.getId());
        } catch (AutoResponderException e) { log.error("Failed to trigger auto-responder for chat ID {}: {}", savedChat.getId(), e.getMessage(), e);
            throw new AutoResponderException(e.getMessage());
        }

        return chatMapper.toDetailsDto(savedChat);
    }

    @Override
    @Transactional
    public ChatDetailsDTO requestOperatorEscalation(Integer chatId, Integer clientId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Chat with ID " + chatId + " not found"));
        log.debug("Found chat ID {} with status {}", chat.getId(), chat.getStatus());

        if (!chat.getClient().getId().equals(clientId)) {
            log.warn("Client ID {} attempted to request operator for chat ID {} which belongs to client ID {}",
                    clientId, chatId, chat.getClient().getId());
            throw new ChatServiceException("You are not the client of this chat.");
        }

        if (chat.getStatus() == ChatStatus.CLOSED || chat.getStatus() == ChatStatus.ARCHIVED) {
            throw new ChatServiceException("Chat ID " + chatId + " is already closed.");
        }

        autoResponderService.stopForChat(chatId);

        if (chat.getUser() == null) {
            Optional<User> operator = assignmentService.findLeastBusyOperator(chat.getCompany().getId());
            if (operator.isPresent()) {
                chat.setUser(operator.get());
                notificationService.createNotification(operator.get(), chat,
                        "CHAT_ASSIGNED", "Вам назначен чат #" + chat.getId());
            } else {
                messagingService.sendMessage("/topic/operators/available/new-chats", chatMapper.toDto(chat));
            }
        }

        chat.setStatus(ChatStatus.ASSIGNED);
        Chat updatedChat = chatRepository.save(chat);

        messagingService.sendMessage("/topic/chat/" + chat.getId() + "/status", chatMapper.toDto(updatedChat));

        return chatMapper.toDetailsDto(updatedChat);
    }

    @EventListener
    public void handleAutoResponderEscalationEvent(AutoResponderEscalationEvent event) {
        log.info("Received AutoResponderEscalationEvent for chat ID: {}", event.getChatId());
        try {
            requestOperatorEscalation(event.getChatId(), event.getClientId());
            log.info("Handled escalation event successfully for chat ID: {}", event.getChatId());
        } catch (Exception e) {
            log.error("Error handling escalation event for chat ID {}: {}", event.getChatId(), e.getMessage(), e);
            throw new ChatServiceException(e);
        }
    }

    @Override
    @Transactional
    public void linkOperatorToChat(Integer chatId, Integer operatorId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Chat with ID " + chatId + " not found for linking operator."));

        User operator = userService.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator with ID " + operatorId + " not found for linking."));

        if (chat.getCompany() == null || operator.getCompany() == null || !chat.getCompany().getId().equals(operator.getCompany().getId())) {
            log.warn("Attempted to link operator {} from company {} to chat {} from company {}",
                    operatorId, operator.getCompany() != null ? operator.getCompany().getId() : "none",
                    chatId, chat.getCompany() != null ? chat.getCompany().getId() : "none");
            throw new ChatServiceException("Operator and chat must belong to the same company to link.");
        }


        chat.setUser(operator);

        chatRepository.save(chat);
        log.info("Operator {} linked to chat {}", operatorId, chatId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Chat> findOpenChatByClientAndChannel(Integer clientId, ChatChannel channel) {
        Optional<Chat> foundChat = chatRepository
                .findFirstByClientIdAndChatChannelAndStatusInOrderByCreatedAtDesc(clientId, channel, OPEN_CHAT_STATUSES);

        if(foundChat.isPresent()) {
            log.debug("Found open chat ID {} for client {} on channel {}", foundChat.get().getId(), clientId, channel);
        } else {
            log.debug("No open chat found for client {} on channel {}", clientId, channel);
        }
        return foundChat;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Chat> findOpenChatByClient(Integer clientId) {
        return chatRepository.findByClientId(clientId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Chat> findChatEntityById(Integer chatId) {
        log.debug("Finding chat entity by ID: {}", chatId);
        return chatRepository.findById(chatId);
    }

    @Override
    @Transactional
    public ChatDetailsDTO assignOperatorToChat(AssignChatRequestDTO assignRequest) {
        Chat chat = chatRepository.findById(assignRequest.getChatId())
                .orElseThrow(() -> new ChatNotFoundException("Chat with ID " + assignRequest.getChatId() + " not found"));

        if (chat.getStatus() != ChatStatus.PENDING_OPERATOR) {
            throw new ChatServiceException("Chat with ID " + assignRequest.getChatId() + " is not in a state to be assigned.");
        }

        // TODO: Проверить права текущего пользователя. Только оператор может взять чат.

        User operatorToAssign;
        if (assignRequest.getOperatorId() != null) {
            log.debug("Assigning specific operator with ID: {}", assignRequest.getOperatorId());
            operatorToAssign = userService.findById(assignRequest.getOperatorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Operator with ID " + assignRequest.getOperatorId() + " not found."));
            // TODO: Проверить, что operatorToAssign действительно является оператором и доступен
        } else {
            //TODO Если ID оператора не указан в запросе, найти наименее загруженного
            Optional<User> autoAssignedOperator = assignmentService.assignOperator(chat);
            operatorToAssign = autoAssignedOperator.orElseThrow(() -> new ResourceNotFoundException("No available operator found for assignment for company " + chat.getCompany().getId()));
            log.debug("Auto-assigned operator with ID: {}", operatorToAssign.getId());
        }

        if (chat.getStatus() == ChatStatus.ASSIGNED && chat.getUser() != null && chat.getUser().getId().equals(operatorToAssign.getId())) {
            log.info("Chat {} is already assigned to operator {}", chat.getId(), operatorToAssign.getId());
            return chatMapper.toDetailsDto(chatRepository.findById(chat.getId()).orElseThrow());
        }

        chat.setUser(operatorToAssign);
        chat.setStatus(ChatStatus.ASSIGNED);
        chat.setAssignedAt(LocalDateTime.now());

        Chat updatedChat = chatRepository.save(chat);
        log.info("Chat ID {} assigned to operator {}. New status: {}",
                updatedChat.getId(), operatorToAssign.getId(), updatedChat.getStatus());

        notificationService.createNotification(operatorToAssign, updatedChat, "CHAT_ASSIGNED", "Вам назначен чат #" + updatedChat.getId());

        if (chat.getStatus() == ChatStatus.PENDING_OPERATOR) {
            messagingService.sendMessage("/topic/operators/available/chat-assignment", chatMapper.toDto(updatedChat));
            log.debug("Sent chat assignment notification to /topic/operators/available/chat-assignment for chat ID {}", updatedChat.getId());
        }

        Chat chatWithDetails = chatRepository.findById(updatedChat.getId())
                .orElseThrow(() -> new RuntimeException("Failed to load assigned chat details after saving"));

        log.info("Chat assignment process finished for chat ID: {}", chatWithDetails.getId());
        return chatMapper.toDetailsDto(chatWithDetails);
    }

    @Override
    @Transactional
    public ChatDetailsDTO closeChat(CloseChatRequestDTO closeRequest) {
        Chat chat = chatRepository.findById(closeRequest.getChatId())
                .orElseThrow(() -> new ChatNotFoundException("Chat with ID " + closeRequest.getChatId() + " not found"));

        if (chat.getStatus() == ChatStatus.CLOSED || chat.getStatus() == ChatStatus.ARCHIVED) {
            log.warn("Chat ID {} is already closed or archived. Status: {}", chat.getId(), chat.getStatus());
            throw new ChatServiceException("Chat with ID " + closeRequest.getChatId() + " is already closed or archived.");
        }

        chat.setStatus(ChatStatus.CLOSED);
        chat.setClosedAt(LocalDateTime.now());

        Chat closedChat = chatRepository.save(chat);
        log.info("Chat ID {} status changed to CLOSED at {}", closedChat.getId(), closedChat.getClosedAt());

        if (closedChat.getUser() != null) {
            notificationService.createNotification(closedChat.getUser(), closedChat, "CHAT_CLOSED", "Чат #" + closedChat.getId() + " был закрыт.");
            log.debug("Created notification for operator {} about chat closure.", closedChat.getUser().getId());
        }

        messagingService.sendMessage("/topic/chat/" + closedChat.getId() + "/status", chatMapper.toDto(closedChat));
        log.debug("Sent chat closure notification to /topic/chat/{}/status", closedChat.getId());

        Chat chatWithDetails = chatRepository.findById(closedChat.getId()) // Загрузить снова для деталей
                .orElseThrow(() -> new RuntimeException("Failed to load closed chat details after saving")); // Не должно произойти

        log.info("Chat closure process finished for chat ID: {}", chatWithDetails.getId());
        return chatMapper.toDetailsDto(chatWithDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatDetailsDTO getChatDetails(Integer chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Chat with ID " + chatId + " not found"));

        return chatMapper.toDetailsDto(chat);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatDTO> getOperatorChats(Integer userId) {
        Collection<ChatStatus> assignedStatuses = Set.of(ChatStatus.ASSIGNED, ChatStatus.IN_PROGRESS);
        List<Chat> chats = chatRepository.findByUserIdAndStatusIn(userId, assignedStatuses);

        return chats.stream()
                .map(chatMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatDTO> getOperatorChatsStatus(Integer userId, Set<ChatStatus> statuses) {
        List<Chat> chats = chatRepository.findByUserIdAndStatusIn(userId, statuses);
        return chats.stream()
                .map(chatMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatDTO> getClientChats(Integer clientId) {
        // TODO: Реализовать получение чатов для клиента.
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatDTO> getWaitingChats(Integer companyId) {
        List<Chat> chats = chatRepository.findByCompanyIdAndStatusOrderByLastMessageAtDesc(companyId, ChatStatus.PENDING_OPERATOR);
        return chats.stream()
                .map(chatMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateChatStatus(Integer chatId, ChatStatus newStatus) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Chat with ID " + chatId + " not found"));
        chat.setStatus(newStatus);
        if (newStatus == ChatStatus.CLOSED) {
            chat.setClosedAt(LocalDateTime.now());
        } else if (newStatus == ChatStatus.ASSIGNED) {
            if (chat.getAssignedAt() == null) chat.setAssignedAt(LocalDateTime.now());
        }

        chatRepository.save(chat);

        messagingService.sendMessage("/topic/chat/" + chatId + "/status", chatMapper.toDto(chat));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Chat> findChatByExternalId(Integer companyId, Integer clientId, ChatChannel channel, String externalChatId) {
        throw new UnsupportedOperationException("findChatByExternalId Not implemented yet");
    }

    private Chat createChatEntity(Client client, Company company, ChatChannel channel) {
        Chat chat = new Chat();
        chat.setClient(client);
        chat.setCompany(company);
        chat.setChatChannel(channel);
        chat.setStatus(ChatStatus.PENDING_AUTO_RESPONDER);
        chat.setCreatedAt(LocalDateTime.now());
        chat.setUser(null);
        chat.setAssignedAt(null);
        chat.setClosedAt(null);
        log.debug("Created initial chat entity.");

        return chat;
    }

    private ChatMessage createChatMessageEntity(Chat chat,
                                                Client senderClient,
                                                User senderOperator,
                                                ChatMessageSenderType senderType,
                                                String content,
                                                String externalMessageId,
                                                String replyToExternalMessageId) {
        ChatMessage message = new ChatMessage();
        message.setChat(chat);
        message.setSenderClient(senderClient);
        message.setSenderOperator(senderOperator);
        message.setSenderType(senderType);
        message.setContent(content);
        message.setExternalMessageId(externalMessageId);
        message.setReplyToExternalMessageId(replyToExternalMessageId);
        message.setSentAt(LocalDateTime.now());
        message.setStatus(MessageStatus.SENT);

        return message;
    }
}

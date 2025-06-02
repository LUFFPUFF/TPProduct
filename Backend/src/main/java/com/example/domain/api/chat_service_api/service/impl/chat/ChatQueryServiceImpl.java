package com.example.domain.api.chat_service_api.service.impl.chat;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.crm_module.client.Client;
import com.example.database.repository.chats_messages_module.ChatMessageRepository;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.domain.api.chat_service_api.exception_handler.ChatNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.mapper.ChatMapper;
import com.example.domain.api.chat_service_api.model.dto.ChatDTO;
import com.example.domain.api.chat_service_api.model.dto.ChatDetailsDTO;
import com.example.domain.api.company_module.service.IClientService;
import com.example.domain.api.company_module.service.IUserService;
import com.example.domain.api.chat_service_api.service.chat.IChatQueryService;
import com.example.domain.api.chat_service_api.util.ChatValidationUtil;
import com.example.domain.api.chat_service_api.util.MdcUtil;
import com.example.domain.security.model.UserContext;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.nio.file.AccessDeniedException;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.domain.api.chat_service_api.service.impl.LeastBusyAssignmentService.OPEN_CHAT_STATUSES;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatQueryServiceImpl implements IChatQueryService {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMapper chatMapper;
    private final IUserService userService;
    private final ChatValidationUtil chatValidationUtil;
    private final IClientService clientService;

    private static final String KEY_CHAT_ID = "chatId";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_CLIENT_ID = "clientId";
    private static final String KEY_OPERATOR_ID = "operatorId";
    private static final String KEY_COMPANY_ID = "companyId";
    private static final String KEY_STATUSES = "statuses";

    private static final String OPERATION_GET_CHAT_DETAILS = "getChatDetails";
    private static final String OPERATION_FIND_OPEN_CHAT_CLIENT_CHANNEL = "findOpenChatEntityByClientAndChannel";
    private static final String OPERATION_FIND_OPEN_CHAT_CLIENT_CHANNEL_EXTID = "findOpenChatEntityByClientAndChannelAndExternalId";
    private static final String OPERATION_FIND_CHAT_ENTITY_ID = "findChatEntityById";
    private static final String OPERATION_GET_MY_VIEWABLE_CHATS = "getMyViewableChats";
    private static final String OPERATION_GET_MY_ASSIGNED_CHATS = "getMyAssignedChats";
    private static final String OPERATION_GET_OPERATOR_CHATS = "getOperatorChats";
    private static final String OPERATION_GET_CLIENT_CHATS = "getClientChats";
    private static final String OPERATION_GET_COMPANY_PENDING_CHATS = "getMyCompanyPendingOperatorChats";
    private static final String OPERATION_FIND_CHAT_EXTID = "findChatEntityByExternalId";
    private static final String OPERATION_FIND_FIRST_MESSAGE = "findFirstMessageEntityByChatId";

    private static final Set<ChatStatus> DEFAULT_MANAGER_VIEWABLE_STATUSES = Set.of(
            ChatStatus.ASSIGNED, ChatStatus.IN_PROGRESS, ChatStatus.PENDING_OPERATOR, ChatStatus.PENDING_AUTO_RESPONDER
    );
    private static final Set<ChatStatus> DEFAULT_OPERATOR_ASSIGNED_STATUSES = Set.of(
            ChatStatus.ASSIGNED, ChatStatus.IN_PROGRESS
    );


    @Override
    @Transactional(readOnly = true)
    public ChatDetailsDTO getChatDetails(Integer chatId, UserContext userContext) throws AccessDeniedException {
        return MdcUtil.withContext(
                () -> {
                    log.debug("Fetching details for chat ID {} by user {}", chatId, userContext.getUserId());
                    Chat chat = chatRepository.findByIdWithMessages(chatId)
                            .orElseThrow(() -> new ChatNotFoundException("Chat with ID " + chatId + " not found."));

                    chatValidationUtil.ensureChatBelongsToCompany(chat, userContext.getCompanyId(), OPERATION_GET_CHAT_DETAILS);

                    return chatMapper.toDetailsDto(chat);
                },
                "operation", OPERATION_GET_CHAT_DETAILS,
                KEY_CHAT_ID, chatId,
                KEY_USER_ID, userContext.getUserId()
        );
    }

    @Override
    public Optional<Chat> findOpenChatEntityByClientAndChannel(Integer clientId, ChatChannel channel) {
        return MdcUtil.withContext(
                () -> {
                    log.debug("Finding open chat entity for client {} on channel {}", clientId, channel);
                    return chatRepository.findFirstByClientIdAndChatChannelAndStatusInOrderByCreatedAtDesc(
                            clientId, channel, OPEN_CHAT_STATUSES
                    );
                },
                "operation", OPERATION_FIND_OPEN_CHAT_CLIENT_CHANNEL,
                KEY_CLIENT_ID, clientId,
                "chatChannel", channel != null ? channel.name() : "null"
        );
    }

    @Override
    public Optional<Chat> findOpenChatEntityByClientAndChannelAndExternalId(Integer clientId, ChatChannel channel, String externalChatId) {
        return MdcUtil.withContext(
                () -> {
                    if (clientId == null || channel == null || externalChatId == null || externalChatId.trim().isEmpty()) {
                        return Optional.empty();
                    }
                    log.debug("Finding open chat entity for client {} on channel {} with external ID {}",
                            clientId, channel, externalChatId);
                    return chatRepository.findFirstByClientIdAndChatChannelAndExternalChatIdAndStatusInOrderByCreatedAtDesc(
                            clientId, channel, externalChatId, OPEN_CHAT_STATUSES
                    );
                },
                "operation", OPERATION_FIND_OPEN_CHAT_CLIENT_CHANNEL_EXTID,
                KEY_CLIENT_ID, clientId,
                "chatChannel", channel != null ? channel.name() : "null",
                "externalChatId", externalChatId
        );
    }

    @Override
    public Optional<ChatMessage> findFirstMessageEntityByChatId(Integer chatId, UserContext userContext) throws AccessDeniedException {
        return MdcUtil.withContext(
                () -> {
                    String requesterInfo = (userContext != null && userContext.getUserId() != null) ? "User " + userContext.getUserId() : "System";
                    log.debug("{} requesting first message for chat ID {}", requesterInfo, chatId);

                    if (userContext != null) {
                        Chat chat = this.findChatEntityById(chatId)
                                .orElseThrow(() -> new ChatNotFoundException("Chat with ID " + chatId + " not found when attempting to find its first message."));

                        chatValidationUtil.ensureChatBelongsToCompany(chat, userContext.getCompanyId(), OPERATION_FIND_FIRST_MESSAGE);

                    }

                    Optional<ChatMessage> firstMessage = chatMessageRepository.findFirstByChatIdOrderBySentAtAsc(chatId);
                    if (firstMessage.isPresent()) {
                        log.info("Found first message ID {} for chat ID {}", firstMessage.get().getId(), chatId);
                    } else {
                        log.info("No messages found for chat ID {} when searching for the first message.", chatId);
                    }
                    return firstMessage;
                },
                "operation", OPERATION_FIND_FIRST_MESSAGE,
                KEY_CHAT_ID, chatId,
                KEY_USER_ID, (userContext != null ? userContext.getUserId() : "System")
        );
    }

    @Override
    public Optional<Chat> findChatEntityById(Integer chatId) {
        return MdcUtil.withContext(
                () -> {
                    log.debug("Finding chat entity by ID: {}", chatId);
                    return chatRepository.findById(chatId);
                },
                "operation", OPERATION_FIND_CHAT_ENTITY_ID,
                KEY_CHAT_ID, chatId
        );
    }

    @Override
    public List<ChatDTO> getMyViewableChats(Set<ChatStatus> statuses, UserContext userContext) throws AccessDeniedException {
        return MdcUtil.withContext(
                () -> {
                    log.debug("User ID {} (Roles: {}, Company: {}) requesting their viewable chats with statuses: {}",
                            userContext.getUserId(), userContext.getRoles(), userContext.getCompanyId(), statuses);

                    if (userContext.getCompanyId() == null) {
                        throw new AccessDeniedException("User is not associated with a company.");
                    }

                    List<Chat> chats;
                    Collection<ChatStatus> effectiveStatuses = CollectionUtils.isEmpty(statuses) ?
                            determineDefaultViewableStatuses(userContext) : statuses;

                    if (userContext.getRoles().contains(Role.MANAGER)) {
                        chats = chatRepository.findByCompanyIdAndStatusInOrderByLastMessageAtDesc(userContext.getCompanyId(), effectiveStatuses);
                    } else if (userContext.getRoles().contains(Role.OPERATOR)) {
                        chats = chatRepository.findByUserIdAndStatusIn(userContext.getUserId(), effectiveStatuses);
                    } else {
                        throw new AccessDeniedException("Access Denied: Your role does not permit listing chats in this context.");
                    }
                    return mapChatsToDtoWithSnippets(chats);
                },
                "operation", OPERATION_GET_MY_VIEWABLE_CHATS,
                KEY_USER_ID, userContext.getUserId(),
                KEY_COMPANY_ID, userContext.getCompanyId(),
                KEY_STATUSES, statuses != null ? statuses.stream().map(Enum::name).collect(Collectors.joining(",")) : "default"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatDTO> getMyAssignedChats(Set<ChatStatus> statuses, UserContext userContext) {
        return MdcUtil.withContext(
                () -> {
                    log.debug("User ID {} (Company: {}) requesting their assigned chats with statuses: {}",
                            userContext.getUserId(), userContext.getCompanyId(), statuses);

                    Collection<ChatStatus> effectiveStatuses = CollectionUtils.isEmpty(statuses) ?
                            DEFAULT_OPERATOR_ASSIGNED_STATUSES : statuses;

                    List<Chat> chats = chatRepository.findByUserIdAndStatusInWithMessages(userContext.getUserId(), effectiveStatuses);
                    return mapChatsToDtoWithSnippets(chats);
                },
                "operation", OPERATION_GET_MY_ASSIGNED_CHATS,
                KEY_USER_ID, userContext.getUserId(),
                KEY_STATUSES, statuses != null ? statuses.stream().map(Enum::name).collect(Collectors.joining(",")) : "default"
        );
    }

    @Override
    public List<ChatDTO> getOperatorChats(Integer operatorId, Set<ChatStatus> statuses, UserContext userContext) throws AccessDeniedException {
        return MdcUtil.withContext(
                () -> {
                    log.debug("User {} requesting chats for operator {} with statuses: {}",
                            userContext.getUserId(), operatorId, statuses);

                    User targetOperator = userService.findById(operatorId)
                            .orElseThrow(() -> new ResourceNotFoundException("Operator with ID " + operatorId + " not found."));

                    if (targetOperator.getCompany() == null || userContext.getCompanyId() == null ||
                            !Objects.equals(userContext.getCompanyId(), targetOperator.getCompany().getId())) {
                        throw new AccessDeniedException("Access Denied: Cannot view chats for an operator in a different company.");
                    }
                    if (!userContext.getRoles().contains(Role.MANAGER)) {
                        throw new AccessDeniedException("Access Denied: Only managers can view other operators' chats.");
                    }

                    Collection<ChatStatus> effectiveStatuses = CollectionUtils.isEmpty(statuses) ?
                            DEFAULT_OPERATOR_ASSIGNED_STATUSES : statuses;

                    List<Chat> chats = chatRepository.findByUserIdAndStatusIn(operatorId, effectiveStatuses);
                    return chats.stream().map(chatMapper::toDto).toList();
                },
                "operation", OPERATION_GET_OPERATOR_CHATS,
                KEY_OPERATOR_ID, operatorId,
                KEY_USER_ID, userContext.getUserId(),
                KEY_STATUSES, statuses != null ? statuses.stream().map(Enum::name).collect(Collectors.joining(",")) : "default"
        );
    }

    @Override
    public List<ChatDTO> getClientChats(Integer requestedClientId, UserContext userContext) throws AccessDeniedException {
        return MdcUtil.withContext(
                () -> {
                    if (userContext.getUserId() == null || userContext.getCompanyId() == null) {
                        throw new AccessDeniedException("Access Denied: Invalid user session or insufficient context information.");
                    }

                    log.debug("User {} (Company: {}) requesting chats for client ID {}",
                            userContext.getUserId(), userContext.getCompanyId(), requestedClientId);

                    Client client = clientService.findById(requestedClientId)
                            .orElseThrow(() -> {
                                log.warn("[{}] Client with ID {} not found when requested by user {}.",
                                        OPERATION_GET_CLIENT_CHATS, requestedClientId, userContext.getUserId());
                                return new ResourceNotFoundException("Client with ID " + requestedClientId + " not found.");
                            });

                    if (client.getCompany() == null || !Objects.equals(client.getCompany().getId(), userContext.getCompanyId())) {
                        throw new AccessDeniedException("Access Denied: You can only view chats of clients belonging to your company.");
                    }

                    List<Chat> clientChats;

                    if (userContext.getRoles().contains(Role.OPERATOR) && !userContext.getRoles().contains(Role.MANAGER)) {
                        clientChats = chatRepository.findByClientIdAndCompanyIdOrderByCreatedAtDesc(
                                requestedClientId, userContext.getCompanyId()
                        );
                    } else if (userContext.getRoles().contains(Role.MANAGER)) {
                        log.debug("[{}] Manager {} viewing all chats for client {} in company {}.",
                                OPERATION_GET_CLIENT_CHATS, userContext.getUserId(), requestedClientId, userContext.getCompanyId());
                        clientChats = chatRepository.findByClientIdAndCompanyIdOrderByCreatedAtDesc(
                                requestedClientId, userContext.getCompanyId()
                        );
                    } else {

                        clientChats = chatRepository.findByClientIdAndCompanyIdOrderByCreatedAtDesc(
                                requestedClientId, userContext.getCompanyId()
                        );
                    }
                    return mapChatsToDtoWithSnippets(clientChats);
                },
                "operation", OPERATION_GET_CLIENT_CHATS,
                KEY_CLIENT_ID, requestedClientId,
                KEY_USER_ID, userContext.getUserId(),
                KEY_COMPANY_ID, userContext.getCompanyId()
        );
    }

    @Override
    public List<ChatDTO> getMyCompanyPendingOperatorChats(UserContext userContext) throws AccessDeniedException {
        return MdcUtil.withContext(
                () -> {
                    log.debug("User {} requesting PENDING_OPERATOR chats for company {}", userContext.getUserId(), userContext.getCompanyId());

                    if (userContext.getCompanyId() == null) {
                        throw new AccessDeniedException("Access Denied: User is not associated with a company.");
                    }
                     if (!userContext.getRoles().contains(Role.MANAGER)) {
                         throw new AccessDeniedException("Access Denied: Only managers can view company pending chats.");
                     }

                    List<Chat> chats = chatRepository.findByCompanyIdAndStatusOrderByLastMessageAtDesc(
                            userContext.getCompanyId(), ChatStatus.PENDING_OPERATOR
                    );
                    return chats.stream().map(chatMapper::toDto).collect(Collectors.toList());
                },
                "operation", OPERATION_GET_COMPANY_PENDING_CHATS,
                KEY_USER_ID, userContext.getUserId(),
                KEY_COMPANY_ID, userContext.getCompanyId()
        );
    }

    @Override
    public Optional<Chat> findChatEntityByExternalId(Integer companyId, Integer clientId, ChatChannel channel, String externalChatId) {
        return MdcUtil.withContext(
                () -> {
                    log.debug("Finding chat entity by external ID {} for company {}, client {}, channel {}",
                            externalChatId, companyId, clientId, channel);
                    throw new UnsupportedOperationException("findChatEntityByExternalId not implemented yet.");
                },
                "operation", OPERATION_FIND_CHAT_EXTID,
                KEY_COMPANY_ID, companyId,
                KEY_CLIENT_ID, clientId,
                "chatChannel", channel != null ? channel.name() : "null",
                "externalChatId", externalChatId
        );
    }

    private Set<ChatStatus> determineDefaultViewableStatuses(UserContext userContext) {
        if (userContext.getRoles().contains(Role.MANAGER)) {
            return DEFAULT_MANAGER_VIEWABLE_STATUSES;
        } else if (userContext.getRoles().contains(Role.OPERATOR)) {
            return DEFAULT_OPERATOR_ASSIGNED_STATUSES;
        }
        return Collections.emptySet();
    }

    @NotNull
    private List<ChatDTO> mapChatsToDtoWithSnippets(List<Chat> chats) {
        if (CollectionUtils.isEmpty(chats)) {
            return Collections.emptyList();
        }

        return chats.stream()
                .map(chat -> {
                    ChatDTO chatDTO = chatMapper.toDto(chat);
                    if (chat.getMessages() != null && !chat.getMessages().isEmpty()) {
                        try {
                            chatDTO.setLastMessageSnippet(chat.getMessages().get(chat.getMessages().size() - 1).getContent());
                        } catch (Exception e) {
                            log.trace("Could not get last message snippet for chat {}: {}", chat.getId(), e.getMessage());
                            chatDTO.setLastMessageSnippet("");
                        }
                    } else {
                        chatDTO.setLastMessageSnippet("");
                    }
                    return chatDTO;
                })
                .collect(Collectors.toList());
    }
}

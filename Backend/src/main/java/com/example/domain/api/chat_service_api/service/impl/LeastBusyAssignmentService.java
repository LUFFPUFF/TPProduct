package com.example.domain.api.chat_service_api.service.impl;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.company_subscription_module.user_roles.user.UserStatus;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.chat_service_api.service.IAssignmentStrategyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeastBusyAssignmentService implements IAssignmentStrategyService  {

    private final UserRepository userRepository;
    private final ChatRepository chatRepository;

    public static final Set<ChatStatus> OPEN_CHAT_STATUSES = Set.of(
            ChatStatus.ASSIGNED,
            ChatStatus.IN_PROGRESS,
            ChatStatus.PENDING_OPERATOR,
            ChatStatus.PENDING_AUTO_RESPONDER,
            ChatStatus.NEW
    );

    public static final Set<UserStatus> ACCEPTING_CHAT_USER_STATUSES = Set.of(
            UserStatus.ACTIVE
    );

    private static final int DEFAULT_MAX_CONCURRENT_CHATS = 5;

    @Override
    public Optional<User> findOperatorForChat(Chat chatContext) {
        if (chatContext.getCompany() == null || chatContext.getCompany().getId() == null) {
            log.warn("Cannot find operator for chat {} without a valid company context.", chatContext.getId());
            return Optional.empty();
        }

        Integer companyId = chatContext.getCompany().getId();

        List<User> candidateOperators = userRepository.findByCompanyIdAndRoleAndStatusIn(
                companyId,
                Role.OPERATOR,
                ACCEPTING_CHAT_USER_STATUSES
        );

        if (candidateOperators.isEmpty()) {
            log.warn("No operators found with an accepting status (e.g., ACTIVE) for company ID: {}", companyId);
            return Optional.empty();
        }

        List<Integer> candidateOperatorIds = candidateOperators.stream().map(User::getId).collect(Collectors.toList());
        Map<Integer, Long> activeChatsPerOperator = chatRepository.countChatsByUserIdInAndStatusIn(candidateOperatorIds, OPEN_CHAT_STATUSES)
                .stream()
                .collect(Collectors.toMap(
                        ChatRepository.OperatorChatCount::getUserId,
                        ChatRepository.OperatorChatCount::getChatCount
                ));

        Optional<User> leastBusyOperator = candidateOperators.stream()
                .filter(operator -> {
                    long currentChats = activeChatsPerOperator.getOrDefault(operator.getId(), 0L);
                    int maxChats = operator.getMaxConcurrentChats() != null ? operator.getMaxConcurrentChats() : DEFAULT_MAX_CONCURRENT_CHATS;
                    if (currentChats >= maxChats) {
                        log.trace("Operator {} (current chats: {}) has reached or exceeded capacity (max: {}). Skipping.",
                                operator.getId(), currentChats, maxChats);
                        return false;
                    }
                    return true;
                })
                .peek(op -> log.trace("Operator {} is a candidate with {} active chats.", op.getId(), activeChatsPerOperator.getOrDefault(op.getId(), 0L)))
                .min(Comparator.comparingLong(operator -> activeChatsPerOperator.getOrDefault(operator.getId(), 0L)));

        if (leastBusyOperator.isPresent()) {
            log.info("Found least busy and available operator: User ID {} with {} active chats for company ID {}.",
                    leastBusyOperator.get().getId(),
                    activeChatsPerOperator.getOrDefault(leastBusyOperator.get().getId(), 0L),
                    companyId);
        } else {
            log.warn("No suitable operator found for company ID {} (all candidates might be at capacity or none exist).", companyId);
        }

        return leastBusyOperator;

    }
}

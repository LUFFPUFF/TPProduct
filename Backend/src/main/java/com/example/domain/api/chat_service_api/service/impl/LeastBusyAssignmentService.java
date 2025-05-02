package com.example.domain.api.chat_service_api.service.impl;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.chat_service_api.service.IAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LeastBusyAssignmentService implements IAssignmentService {

    private final UserRepository userRepository;

    public static final Collection<ChatStatus> OPEN_CHAT_STATUSES = Set.of(
            ChatStatus.ASSIGNED,
            ChatStatus.IN_PROGRESS,
            ChatStatus.PENDING_OPERATOR,
            ChatStatus.PENDING_AUTO_RESPONDER,
            ChatStatus.NEW
    );

    @Override
    public Optional<User> findLeastBusyOperator(Integer companyId) {
        List<User> users = userRepository.findLeastBusyUser(companyId, OPEN_CHAT_STATUSES);
        return users.stream().findFirst();
    }

    @Override
    public Optional<User> assignOperator(Chat chat) {
        if (chat == null || chat.getCompany() == null) {
            return Optional.empty();
        }
        return findLeastBusyOperator(chat.getCompany().getId());
    }
}

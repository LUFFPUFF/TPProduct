package com.example.domain.api.chat_service_api.service.security.impl;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.model.crm_module.client.Client;
import com.example.database.repository.chats_messages_module.ChatMessageRepository;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.database.repository.crm_module.ClientRepository;
import com.example.domain.api.chat_service_api.service.security.IChatSecurityService;
import com.example.domain.dto.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service("chatSecurityService")
@RequiredArgsConstructor
public class ChatSecurityServiceImpl implements IChatSecurityService {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ClientRepository clientRepository;

    @Override
    public boolean isAppUserOperatorOrManagerWithCompany(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserDetails currentUser)) {
            return false;
        }
        if (currentUser.getCompanyId() == null) {
            return false;
        }
        return currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.OPERATOR.name()) || a.getAuthority().equals(Role.MANAGER.name()));
    }

    /**
     * Проверяет, является ли текущий аутентифицированный principal AppUserDetails.
     */
    private boolean isAppUserPrincipal(Authentication authentication) {
        return authentication != null && (authentication.getPrincipal() instanceof AppUserDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canProcessAndSaveMessage(Integer chatId, Integer senderId, ChatMessageSenderType senderType) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAppUserPrincipal(authentication)) {
            AppUserDetails currentUser  = (AppUserDetails) authentication.getPrincipal();

            if (!isAppUserOperatorOrManagerWithCompany(authentication)) {
                return false;
            }

            if (senderType == ChatMessageSenderType.OPERATOR && currentUser.getId().equals(senderId)) {
                Chat chat = chatRepository.findById(chatId).orElse(null);
                return chat != null && chat.getCompany() != null && chat.getCompany().getId().equals(currentUser.getCompanyId());
            } else {
                return false;
            }
        } else {
            return senderType != ChatMessageSenderType.OPERATOR;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAccessChat(Integer chatId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAppUserOperatorOrManagerWithCompany(authentication)) {
            return false;
        }

        AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

        Chat chat = chatRepository.findById(chatId).orElse(null);
        return chat != null && chat.getCompany() != null && chat.getCompany().getId().equals(currentUser.getCompanyId());
    }

    @Override
    public boolean canAccessClient(Integer clientId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAppUserOperatorOrManagerWithCompany(authentication)) {
            return false;
        }

        AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

        Client client = clientRepository.findById(clientId).orElse(null);
        return client != null && client.getCompany() != null && client.getCompany().getId().equals(currentUser.getCompanyId());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAssignOperatorToChat(Integer chatId) {
        return canAccessChat(chatId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canCloseChat(Integer chatId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAppUserOperatorOrManagerWithCompany(authentication)) {
            return false;
        }

        AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

        Chat chat = chatRepository.findById(chatId).orElse(null);
        return checkChat(currentUser, chat);
    }

    private boolean checkChat(AppUserDetails currentUser, Chat chat) {
        if (chat == null || chat.getCompany() == null || !chat.getCompany().getId().equals(currentUser.getCompanyId())) {
            return false;
        }

        boolean isOperator = currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(Role.OPERATOR.name()));
        boolean isManager = currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(Role.MANAGER.name()));

        if (isOperator) {
            return chat.getUser() != null && chat.getUser().getId().equals(currentUser.getId());
        } else return isManager;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUpdateChatStatus(Integer chatId) {
        return canCloseChat(chatId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUpdateMessageStatus(Integer messageId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAppUserOperatorOrManagerWithCompany(authentication)) {
            return false;
        }

        AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

        return chatMessageRepository.findById(messageId)
                .map(message -> {
                    Chat chat = message.getChat();
                    return chat != null && chat.getCompany() != null && chat.getCompany().getId().equals(currentUser.getCompanyId());
                })
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canMarkMessagesAsRead(Integer chatId, Integer requestedOperatorId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAppUserOperatorOrManagerWithCompany(authentication)) {
            return false;
        }

        AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

        if (!currentUser.getId().equals(requestedOperatorId)) {
            return false;
        }

        Chat chat = chatRepository.findById(chatId).orElse(null);
        return checkChat(currentUser, chat);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUpdateMessageStatusByExternalId(Integer chatId, String externalMessageId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAppUserOperatorOrManagerWithCompany(authentication)) {
            return false;
        }

        AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

        return chatMessageRepository.findByExternalMessageId(externalMessageId)
                .filter(message -> message.getChat().getId().equals(chatId))
                .map(message -> {
                    Chat chat = message.getChat();
                    return chat.getCompany() != null && chat.getCompany().getId().equals(currentUser.getCompanyId());
                })
                .orElse(false);
    }

    @Override
    public Optional<AppUserDetails> getCurrentAppUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && (authentication.getPrincipal() instanceof AppUserDetails)) {
            return Optional.of((AppUserDetails) authentication.getPrincipal());
        }
        return Optional.empty();
    }
}

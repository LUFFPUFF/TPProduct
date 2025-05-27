package com.example.domain.api.chat_service_api.service;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.company_subscription_module.user_roles.user.User;

import java.util.Optional;

public interface IAssignmentStrategyService {

    Optional<User> findOperatorForChat(Chat chat);

}

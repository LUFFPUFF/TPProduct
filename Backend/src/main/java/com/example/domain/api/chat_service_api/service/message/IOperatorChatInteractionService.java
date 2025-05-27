package com.example.domain.api.chat_service_api.service.message;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.database.model.company_subscription_module.user_roles.user.User;

public interface IOperatorChatInteractionService {

    boolean processOperatorMessageImpact(Chat chat, User operator, ChatMessage operatorMessage);
}

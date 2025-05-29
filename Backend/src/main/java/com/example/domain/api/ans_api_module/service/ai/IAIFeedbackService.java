package com.example.domain.api.ans_api_module.service.ai;

import com.example.database.model.ai_module.AIResponses;
import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.database.model.crm_module.client.Client;

import java.util.Optional;

public interface IAIFeedbackService {

    AIResponses logAiResponse(Chat chat, Client client, ChatMessage aiChatMessage, Float confidence);

    void requestFeedbackFromClient(Chat chat, Integer lastAiResponseId);

    void saveClientFeedback(Integer aiResponseId, Integer clientId, double rating, String comment);

    Optional<Integer> findLastLoggedAiResponseIdForChat(Integer chatId);
}

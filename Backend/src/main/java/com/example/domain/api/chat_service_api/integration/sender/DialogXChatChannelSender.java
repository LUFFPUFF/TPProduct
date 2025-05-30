package com.example.domain.api.chat_service_api.integration.sender;

import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.domain.api.chat_service_api.integration.exception.ChannelSenderException;
import com.example.domain.api.chat_service_api.integration.listener.model.SendMessageCommand;
import com.example.domain.api.chat_service_api.integration.manager.widget.DialogXChatManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("dialogXChatChannelSender")
@RequiredArgsConstructor
@Slf4j
public class DialogXChatChannelSender implements ChannelSender {

    private final DialogXChatManager dialogXChatManager;

    @Override
    public void send(SendMessageCommand command) throws ChannelSenderException {
        String clientWidgetSessionId = command.getDialogXChatSessionId();
        Integer companyId = command.getCompanyId();

        if (clientWidgetSessionId == null || clientWidgetSessionId.trim().isEmpty()) {
            String errorMsg = String.format("DialogXChat Session ID not found in SendMessageCommand for company ID %d, chat ID %d",
                    companyId, command.getChatId());
            log.error(errorMsg);
            throw new ChannelSenderException(errorMsg);
        }

        String senderName = command.getSenderDisplayName();
        if (senderName == null || senderName.isBlank()) {
            if (command.getSenderType() == ChatMessageSenderType.OPERATOR) {
                senderName = "Оператор";
            } else if (command.getSenderType() == ChatMessageSenderType.AUTO_RESPONDER) {
                senderName = "Бот поддержки";
            } else {
                senderName = "Поддержка";
            }
        }

        try {
            dialogXChatManager.sendMessageToWidget(
                    companyId,
                    clientWidgetSessionId,
                    command.getContent(),
                    senderName
            );
            log.info("Message passed to DialogXChatManager for session ID {} (company ID {}), sender: '{}'",
                    clientWidgetSessionId, companyId, senderName);
        } catch (Exception e) {
            String errorMsg = String.format("Failed to send DialogXChat message for company ID %d, chat ID %d, session %s: %s",
                    companyId, command.getChatId(), clientWidgetSessionId, e.getMessage());
            log.error(errorMsg, e);
            throw new ChannelSenderException(errorMsg, e);
        }
    }
}

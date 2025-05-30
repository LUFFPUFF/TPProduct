package com.example.domain.api.chat_service_api.integration.sender;

import com.example.domain.api.chat_service_api.integration.exception.ChannelSenderException;
import com.example.domain.api.chat_service_api.integration.listener.model.SendMessageCommand;
import com.example.domain.api.chat_service_api.integration.manager.mail.manager.EmailDialogManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("emailChannelSender")
@RequiredArgsConstructor
@Slf4j
public class EmailChannelSender implements ChannelSender {

    private final EmailDialogManager emailDialogManager;
    private static final String MAIL_EXCEPTION = "Email 'to' or 'from' address not found in SendMessageCommand for company ID %d, chat ID %d";

    @Override
    public void send(SendMessageCommand command) throws ChannelSenderException {
        String toEmailAddress = command.getToEmailAddress();
        String fromEmailAddress = command.getFromEmailAddress();
        String subject = command.getSubject();
        Integer companyId = command.getCompanyId();

        if (toEmailAddress == null || fromEmailAddress == null) {
            String errorMsg = String.format(MAIL_EXCEPTION, companyId, command.getChatId());
            log.error(errorMsg);
            throw new ChannelSenderException(errorMsg);
        }

        try {
            emailDialogManager.sendMessage(companyId, toEmailAddress, subject, command.getContent());
            log.info("Message sent via EmailDialogManager to {} (company ID {})", toEmailAddress, companyId);
        } catch (Exception e) {
            String errorMsg = String.format("Failed to send Email for company ID %d, chat ID %d, to %s: %s",
                    companyId, command.getChatId(), toEmailAddress, e.getMessage());
            log.error(errorMsg, e);
            throw new ChannelSenderException(errorMsg, e);
        }
    }
}

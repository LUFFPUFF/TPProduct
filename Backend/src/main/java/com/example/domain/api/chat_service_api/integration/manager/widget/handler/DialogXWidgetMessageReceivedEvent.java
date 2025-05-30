package com.example.domain.api.chat_service_api.integration.manager.widget.handler;

import com.example.domain.api.chat_service_api.integration.dto.DialogXChatIncomingMessage;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DialogXWidgetMessageReceivedEvent extends ApplicationEvent {

    private final DialogXChatIncomingMessage messagePayload;
    private final String simpSessionId;
    private final String principalName;

    public DialogXWidgetMessageReceivedEvent(Object source,
                                             DialogXChatIncomingMessage messagePayload,
                                             String simpSessionId,
                                             String principalName) {
        super(source);
        this.messagePayload = messagePayload;
        this.simpSessionId = simpSessionId;
        this.principalName = principalName;
    }
}

package com.example.domain.api.chat_service_api.integration.manager.widget.handler;

import com.example.domain.api.chat_service_api.integration.dto.DialogXChatIncomingMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DialogXWidgetMessageHandler {

    private final ApplicationEventPublisher eventPublisher;

    @MessageMapping("/widget/message")
    public void handleWidgetMessage(@Payload @Validated DialogXChatIncomingMessage message,
                                    SimpMessageHeaderAccessor headerAccessor,
                                    Principal principal) {
        String simpSessionId = headerAccessor.getSessionId();
        String principalName = (principal != null) ? principal.getName() : null;

        log.info("Received STOMP message via /widget/message: widgetId={}, clientSessionId={}, text='{}...'. STOMP Session: {}, Principal: {}",
                message.getWidgetId(), message.getSessionId(),
                message.getText().substring(0, Math.min(message.getText().length(), 20)),
                simpSessionId, principalName);

        if (principalName != null && !principalName.equals(message.getSessionId())) {
            log.warn("Principal name '{}' from STOMP authentication differs from clientSessionId '{}' in payload for widgetId {}. " +
                            "Using clientSessionId from payload for application logic.",
                    principalName, message.getSessionId(), message.getWidgetId());
        }

        try {
            DialogXWidgetMessageReceivedEvent event = new DialogXWidgetMessageReceivedEvent(
                    this,
                    message,
                    simpSessionId,
                    principalName
            );
            eventPublisher.publishEvent(event);
            log.debug("Published DialogXWidgetMessageReceivedEvent for widgetId {} (client session {}).",
                    message.getWidgetId(), message.getSessionId());
        } catch (Exception e) {
            log.error("Error publishing DialogXWidgetMessageReceivedEvent for widgetId {}: {}",
                    message.getWidgetId(), e.getMessage(), e);
        }
    }
}

package com.example.domain.api.chat_service_api.integration.manager.widget.listener;

import com.example.domain.api.chat_service_api.integration.manager.widget.handler.DialogXWidgetMessageReceivedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;

@Slf4j
@Component
@RequiredArgsConstructor
public class DialogXWidgetEventListener {

    private final BlockingQueue<Object> incomingMessageQueue;

    @Async
    @EventListener
    public void handleWidgetMessageReceived(DialogXWidgetMessageReceivedEvent event) {
        log.info("Handling DialogXWidgetMessageReceivedEvent for widgetId {} (client session {}). Adding to incomingMessageQueue.",
                event.getMessagePayload().getWidgetId(), event.getMessagePayload().getSessionId());
        try {
            boolean offered = incomingMessageQueue.offer(event.getMessagePayload());
            if (!offered) {
                log.error("Failed to offer message from widget {} (client session {}) to incoming queue from event listener. Queue might be full.",
                        event.getMessagePayload().getWidgetId(), event.getMessagePayload().getSessionId());
            } else {
                log.debug("Message from widget {} (client session {}) successfully added to incoming queue via event listener.",
                        event.getMessagePayload().getWidgetId(), event.getMessagePayload().getSessionId());
            }
        } catch (Exception e) {
            log.error("Error processing DialogXWidgetMessageReceivedEvent for widget {}: {}",
                    event.getMessagePayload().getWidgetId(), e.getMessage(), e);
        }
    }
}

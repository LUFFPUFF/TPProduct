package com.example.domain.api.chat_service_api.integration.listener;

import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.domain.api.chat_service_api.integration.error.FailedMessageRouter;
import com.example.domain.api.chat_service_api.integration.exception.ChannelSenderException;
import com.example.domain.api.chat_service_api.integration.listener.model.SendMessageCommand;
import com.example.domain.api.chat_service_api.integration.sender.ChannelSender;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutgoingMessageQueueListener {

    private final BlockingQueue<Object> outgoingMessageQueue;
    private final Map<ChatChannel, ChannelSender> channelSenders;
    private final FailedMessageRouter failedMessageRouter;

    private volatile boolean running = true;
    private Thread listenerThread;

    @PostConstruct
    public void startListener() {
        listenerThread = new Thread(this::listenForMessages);
        listenerThread.setName("outgoing-msg-listener");
        listenerThread.start();
        log.info("Outgoing message queue listener started.");
    }

    @PreDestroy
    public void stopListener() {
        log.info("Shutting down outgoing message queue listener...");
        running = false;
        if (listenerThread != null) {
            listenerThread.interrupt();
            try {
                listenerThread.join(5000);
                if (listenerThread.isAlive()) {
                    log.warn("Outgoing listener thread did not terminate in time.");
                }
            } catch (InterruptedException e) {
                log.warn("Outgoing message queue listener thread interrupted during shutdown.", e);
                Thread.currentThread().interrupt();
            }
        }
        log.info("Outgoing message queue listener stopped.");
    }

    private void listenForMessages() {
        while (running || !outgoingMessageQueue.isEmpty()) {
            try {
                Object commandObject = outgoingMessageQueue.poll(100, TimeUnit.MILLISECONDS);

                if (commandObject == null) {
                    continue;
                }

                if (!(commandObject instanceof SendMessageCommand sendCommand)) {
                    log.warn("Received unknown command type in outgoing queue: {}. Routing to failed message handler.",
                            commandObject.getClass().getName());
                    failedMessageRouter.routeUnknownObject(commandObject, "Unknown object in outgoing queue", SendMessageCommand.class);
                    continue;
                }

                if (sendCommand.getCompanyId() == null) {
                    log.error("SendMessageCommand received without company ID. ChatID: {}. Channel: {}. Cannot route message.",
                            sendCommand.getChatId(), sendCommand.getChannel());
                    failedMessageRouter.routeFailedCommand(sendCommand, "Missing companyId in command", null);
                    continue;
                }

                log.info("Processing SendMessageCommand for chat ID {} on channel {} (company ID {})",
                        sendCommand.getChatId(), sendCommand.getChannel(), sendCommand.getCompanyId());

                ChannelSender sender = channelSenders.get(sendCommand.getChannel());

                if (sender == null) {
                    String errorMsg = String.format("No ChannelSender configured for channel: %s. Command for chat ID %d.",
                            sendCommand.getChannel(), sendCommand.getChatId());
                    log.error(errorMsg);
                    failedMessageRouter.routeFailedCommand(sendCommand, errorMsg, null);
                    continue;
                }

                try {

                    sender.send(sendCommand);

                } catch (ChannelSenderException e) {
                    failedMessageRouter.routeFailedCommand(sendCommand, "ChannelSenderException: " + e.getMessage(), e.getCause());
                } catch (Exception e) {
                    String errorMsg = String.format("Unexpected error sending message via %s for chat ID %d: %s",
                            sendCommand.getChannel(), sendCommand.getChatId(), e.getMessage());
                    log.error(errorMsg, e);
                    failedMessageRouter.routeFailedCommand(sendCommand, errorMsg, e);
                }

            } catch (InterruptedException e) {
                if (running) {
                    log.warn("Outgoing message queue listener thread interrupted unexpectedly.", e);
                } else {
                    log.info("Outgoing message queue listener thread interrupted during shutdown process.");
                }
                Thread.currentThread().interrupt();
                running = false;
            } catch (Exception e) {
                log.error("Critical error in OutgoingMessageQueueListener while processing queue: {}", e.getMessage(), e);
            }
        }
        log.info("Outgoing message queue listener processing loop finished.");
    }
}

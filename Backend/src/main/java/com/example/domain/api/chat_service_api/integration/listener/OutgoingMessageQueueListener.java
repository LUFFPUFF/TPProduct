package com.example.domain.api.chat_service_api.integration.listener;

import com.example.domain.api.chat_service_api.integration.mail.dialog_bot.EmailDialogBot;
import com.example.domain.api.chat_service_api.integration.telegram.TelegramDialogBot;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutgoingMessageQueueListener {

    private final BlockingQueue<Object> outgoingMessageQueue;

    private final TelegramDialogBot telegramDialogBot;
    private final EmailDialogBot emailDialogBot;

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
        listenerThread.interrupt();
        try {
            listenerThread.join(5000);
        } catch (InterruptedException e) {
            log.warn("Outgoing message queue listener thread interrupted during shutdown.");
            Thread.currentThread().interrupt();
        }

    }

    private void listenForMessages() {
        while (running || !outgoingMessageQueue.isEmpty()) {
            try {
                Object command = outgoingMessageQueue.poll(100, TimeUnit.MILLISECONDS);

                if (command == null) {
                    continue;
                }

                log.debug("Outgoing Listener received command from queue: {}", command);

                if (command instanceof SendMessageCommand sendCommand) {
                    log.info("Processing SendMessageCommand for chat ID {} on channel {}",
                            sendCommand.getChatId(), sendCommand.getChannel());

                    try {
                        switch (sendCommand.getChannel()) {
                            case Telegram:
                                Long telegramChatId = sendCommand.getTelegramChatId();
                                if (telegramChatId != null) {
                                    telegramDialogBot.sendMessage(telegramChatId, sendCommand.getContent());
                                    log.info("Message sent via TelegramDialogBot for chat ID {}", sendCommand.getChatId());
                                } else {
                                    log.error("Telegram Chat ID not found in SendMessageCommand for chat ID {}", sendCommand.getChatId());
                                }
                                break;

                            case Email:
                                String toEmailAddress = sendCommand.getToEmailAddress();
                                String fromEmailAddress = sendCommand.getFromEmailAddress();
                                String subject = sendCommand.getSubject();
                                if (toEmailAddress != null && fromEmailAddress != null) {
                                    emailDialogBot.sendMessage(toEmailAddress, subject, sendCommand.getContent(), fromEmailAddress);
                                    log.info("Message sent via EmailDialogBot for chat ID {}", sendCommand.getChatId());
                                } else {
                                    log.error("Email addresses not found in SendMessageCommand for chat ID {}", sendCommand.getChatId());
                                }
                                break;

                            default:
                                log.error("Unsupported channel in SendMessageCommand: {}", sendCommand.getChannel());
                                // TODO: Возможно, положить команду в очередь ошибок
                                break;
                        }
                    } catch (Exception e) {
                        log.error("Error sending message via adapter for chat ID {}: {}", sendCommand.getChatId(), e.getMessage(), e);
                    }

                } else {
                    log.warn("Received unknown command type in outgoing queue: {}", command.getClass().getName());
                    // TODO: Возможно, положить объект в очередь ошибок
                }

            } catch (InterruptedException e) {
                log.info("Outgoing message queue listener thread interrupted, shutting down.");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Error processing command from outgoing queue: {}", e.getMessage(), e);
                // TODO: Обработать ошибку обработки: залогировать, возможно, положить команду в очередь ошибок
            }
        }
        log.info("Outgoing message queue listener thread finished.");
    }
}

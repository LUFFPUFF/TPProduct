package com.example.domain.api.chat_service_api.integration.listener;

import com.example.domain.api.chat_service_api.integration.mail.dialog_bot.EmailDialogBot;
import com.example.domain.api.chat_service_api.integration.telegram.TelegramBotManager;
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

    private final TelegramBotManager telegramBotManager;
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
                    Integer companyId = sendCommand.getCompanyId();
                    if (companyId == null) {
                        log.error("SendMessageCommand received without company ID. Cannot route message.");
                        continue;
                    }

                    log.info("Processing SendMessageCommand for chat ID {} on channel {} (company ID {})",
                            sendCommand.getChatId(), sendCommand.getChannel(), companyId);

                    try {
                        switch (sendCommand.getChannel()) {
                            case Telegram:
                                Long telegramChatId = sendCommand.getTelegramChatId();
                                if (telegramChatId != null) {
                                    telegramBotManager.sendTelegramMessage(companyId, telegramChatId, sendCommand.getContent());
                                    log.info("Message sent via TelegramBotManager for chat ID {} (company ID {})", telegramChatId, companyId);
                                } else {
                                    log.error("Telegram Chat ID not found in SendMessageCommand for company ID {}", companyId);
                                }
                                break;

                            case Email:
                                String toEmailAddress = sendCommand.getToEmailAddress();
                                String fromEmailAddress = sendCommand.getFromEmailAddress();
                                String subject = sendCommand.getSubject();
                                if (toEmailAddress != null && fromEmailAddress != null) {
                                    emailDialogBot.sendMessage(companyId, toEmailAddress, subject, sendCommand.getContent());
                                    log.info("Message sent via EmailDialogBot for chat ID {} (company ID {})", sendCommand.getChatId(), companyId);
                                } else {
                                    log.error("Email addresses not found in SendMessageCommand for chat ID {} (company ID {})", sendCommand.getChatId(), companyId);
                                }
                                break;

                            default:
                                log.error("Unsupported channel in SendMessageCommand: {}", sendCommand.getChannel());
                                // TODO: Возможно, положить команду в очередь ошибок
                                break;
                        }
                    } catch (Exception e) {
                        log.error("Error sending message via adapter for chat ID {} (company ID {}): {}", sendCommand.getChatId(), companyId, e.getMessage(), e);
                        // TODO: Обработка ошибки: залогировать, возможно, положить команду обратно или в очередь ошибок
                    }

                } else {
                    log.warn("Received unknown command type in outgoing queue: {}", command.getClass().getName());
                    // TODO: Возможно, положить объект в очередь ошибок
                }

            } catch (InterruptedException e) {
                log.info("Outgoing message queue listener thread interrupted, shutting down.");
                Thread.currentThread().interrupt();
                running = false;
            } catch (Exception e) {
                log.error("Error processing command from outgoing queue: {}", e.getMessage(), e);
                // TODO: Обработать ошибку обработки: залогировать, возможно, положить команду в очередь ошибок
            }
        }
    }
}

package com.example.domain.api.chat_service_api.integration.listener;

import com.example.domain.api.chat_service_api.integration.mail.response.EmailResponse;
import com.example.domain.api.chat_service_api.integration.process_service.ClientCompanyProcessService;
import com.example.domain.api.chat_service_api.integration.telegram.TelegramResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;

@Component
@RequiredArgsConstructor
@Slf4j
public class IncomingMessageQueueListener {

    private final BlockingQueue<Object> incomingMessageQueue;
    private final ClientCompanyProcessService clientCompanyProcessService;

    private volatile boolean running = true;
    private Thread listenerThread;

    @PostConstruct
    public void startListener() {
        listenerThread = new Thread(this::listenForMessages);
        listenerThread.setName("incoming-msg-listener");
        listenerThread.start();
        log.info("Incoming message queue listener started.");
    }

    @PreDestroy
    public void stopListener() {
        log.info("Shutting down incoming message queue listener...");
        running = false;
        listenerThread.interrupt();
        try {
            listenerThread.join(5000);
        } catch (InterruptedException e) {
            log.warn("Incoming message queue listener thread interrupted during shutdown.");
            Thread.currentThread().interrupt();
        }
        log.info("Incoming message queue listener stopped.");
    }

    private void listenForMessages() {
        while (running || !incomingMessageQueue.isEmpty()) {
            try {
                Object message = incomingMessageQueue.take();

                if (message instanceof TelegramResponse telegramMessage) {
                    clientCompanyProcessService.processTelegram(telegramMessage);
                } else if (message instanceof EmailResponse emailMessage) {
                    clientCompanyProcessService.processEmail(emailMessage.getTo(), emailMessage);
                } else {
                    log.warn("Received unknown message type in queue: {}", message.getClass().getName());
                }

            } catch (InterruptedException e) {
                log.info("Incoming message queue listener thread interrupted, shutting down.");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error processing message from queue: {}", e.getMessage(), e);
            }
        }
    }
}

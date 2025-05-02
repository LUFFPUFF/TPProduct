package com.example.domain.api.chat_service_api.integration.telegram;

import com.example.database.model.company_subscription_module.company.CompanyTelegramConfiguration;
import com.example.database.repository.company_subscription_module.CompanyTelegramConfigurationRepository;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
@Slf4j
public class TelegramDialogBot extends TelegramLongPollingBot {

    private final BlockingQueue<Object> incomingMessageQueue;
    private final CompanyTelegramConfigurationRepository companyTelegramConfigurationRepository;
    private final String botUsername;
    private volatile boolean running = true;
    private BotSession botSession;
    private Long botId;

    private static final int MAX_RETRY_ATTEMPTS = 10;
    private static final long RETRY_DELAY_MS = 10000;

    public TelegramDialogBot(
            @Value("${telegram.token}") String botToken,
            @Value("${telegram.username}") String botUsername,
            BlockingQueue<Object> incomingMessageQueue,
            CompanyTelegramConfigurationRepository companyTelegramConfigurationRepository) {

        super(botToken);
        this.botUsername = botUsername;
        this.incomingMessageQueue = incomingMessageQueue;
        this.companyTelegramConfigurationRepository = companyTelegramConfigurationRepository;
    }

    @PostConstruct
    public void init() {
        startBotWithRetry();
        startMessageProcessingThread();
        fetchBotInfo();
    }

    @PreDestroy
    public void destroy() {
        running = false;
        if (botSession != null) {
            botSession.stop();
        }
    }

    private void startMessageProcessingThread() {
        Thread messageProcessor = new Thread(() -> {
            while (running) {
                try {
                    processPendingMessages();
                } catch (Exception e) {
                    log.error("Error in message processing thread", e);
                }
            }
        });
        messageProcessor.setName("TelegramMessageProcessor");
        messageProcessor.setDaemon(true);
        messageProcessor.start();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        Message message = update.getMessage();
        User user = message.getFrom();

        if (user == null || user.getIsBot()) {
            return;
        }

        try {
            TelegramResponse response = buildTelegramResponse(message);
            updateChatConfiguration(message.getChatId());
            incomingMessageQueue.put(response);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Message processing interrupted");
        } catch (Exception e) {
            log.error("Error processing Telegram message", e);
        }
    }

    private void processPendingMessages() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void fetchBotInfo() {
        try {
            User botInfo = execute(new GetMe());
            this.botId = botInfo.getId();
            log.info("Bot info fetched. ID: {}, Username: {}", botId, botUsername);
        } catch (TelegramApiException e) {
            log.error("Failed to fetch bot info", e);
        }
    }


    private void startBotWithRetry() {
        TelegramBotsApi botsApi;
        try {
            botsApi = new TelegramBotsApi(DefaultBotSession.class);
        } catch (TelegramApiException e) {
            log.error("Failed to create TelegramBotsApi instance", e);
            throw new RuntimeException("Failed to create TelegramBotsApi", e);
        }

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                botSession = botsApi.registerBot(this);
                log.info("Successfully registered Telegram bot @{}", botUsername);
                return;
            } catch (TelegramApiException e) {
                log.error("Failed to register bot (attempt {}/{}): {}",
                        attempt, MAX_RETRY_ATTEMPTS, e.getMessage());

                if (attempt == MAX_RETRY_ATTEMPTS) {
                    throw new RuntimeException("Failed to register bot after " + MAX_RETRY_ATTEMPTS + " attempts", e);
                }

                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Bot registration interrupted", ie);
                }
            }
        }
    }

    private TelegramResponse buildTelegramResponse(Message message) {
        return TelegramResponse.builder()
                .botId(botId)
                .botUsername(botUsername)
                .username(message.getFrom().getUserName())
                .firstUsername(message.getFrom().getFirstName())
                .text(message.getText())
                .date(message.getDate())
                .build();
    }

    private void updateChatConfiguration(Long chatId) {
        companyTelegramConfigurationRepository.findByBotUsername(botUsername)
                .ifPresent(config -> {
                    if (!chatId.equals(config.getChatTelegramId())) {
                        config.setChatTelegramId(chatId);
                        companyTelegramConfigurationRepository.save(config);
                        log.info("Updated chat ID for bot {} to {}", botUsername, chatId);
                    }
                });
    }

    /**
     * Отправляет текстовое сообщение в указанный чат Telegram.
     * @param telegramChatId ID чата Telegram (Long).
     * @param text Содержимое сообщения.
     */
    public void sendMessage(Long telegramChatId, String text) {
        if (telegramChatId == null || text == null || text.trim().isEmpty()) {
            return;
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(telegramChatId);
        sendMessage.setText(text);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException("Failed to send Telegram message", e);
        }

    }

    /**
     * Отправляет фото в указанный чат Telegram.
     * @param chatId ID чата Telegram (Long).
     * @param photoUrl путь к изображению.
     */
    public void sendPhoto(Long chatId, String photoUrl, String caption) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId.toString());
        sendPhoto.setPhoto(new InputFile(photoUrl));
        sendPhoto.setCaption(caption);

        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            log.error("Failed to send photo", e);
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }
}

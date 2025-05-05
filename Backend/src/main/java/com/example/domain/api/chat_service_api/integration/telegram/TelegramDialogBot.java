package com.example.domain.api.chat_service_api.integration.telegram;

import com.example.database.model.company_subscription_module.company.CompanyTelegramConfiguration;
import com.example.database.repository.company_subscription_module.CompanyTelegramConfigurationRepository;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class TelegramDialogBot {

    private final String botToken;
    private final String botUsername;
    private final BlockingQueue<Object> incomingMessageQueue;
    private final CompanyTelegramConfigurationRepository companyTelegramConfigurationRepository;
    private volatile boolean running = true;
    private Long botId;
    private Thread pollingThread;

    private static final long RETRY_DELAY_MS = 10000;
    private static final long POLLING_INTERVAL_MS = 1000;
    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot";

    private final RestTemplate restTemplate = new RestTemplate();
    private final AtomicBoolean pollingActive = new AtomicBoolean(false);

    public TelegramDialogBot(String botToken,
                             String botUsername,
                             BlockingQueue<Object> incomingMessageQueue,
                             CompanyTelegramConfigurationRepository companyTelegramConfigurationRepository) {

        this.botToken = botToken;
        this.botUsername = botUsername;
        this.incomingMessageQueue = incomingMessageQueue;
        this.companyTelegramConfigurationRepository = companyTelegramConfigurationRepository;
    }

    @PostConstruct
    public void init() {
        if (pollingActive.compareAndSet(false, true)) {
            fetchBotInfo();
            startPolling();
            startMessageProcessingThread();
        } else {
            log.warn("Bot is already running");
        }
    }

    @PreDestroy
    public void destroy() {
        if (pollingActive.compareAndSet(true, false)) {
            running = false;
            if (pollingThread != null) {
                pollingThread.interrupt();
                try {
                    pollingThread.join(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            log.info("Telegram bot stopped successfully");
        }
    }

    private void startPolling() {
        pollingThread = new Thread(() -> {
            long offset = 0;
            while (running && pollingActive.get()) {
                try {
                    List<Update> updates = getUpdates(offset);
                    for (Update update : updates) {
                        processUpdate(update);
                        offset = update.getUpdateId() + 1;
                    }
                    Thread.sleep(POLLING_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.info("Polling thread interrupted");
                    break;
                } catch (Exception e) {
                    handlePollingError(e);
                }
            }
        });
        pollingThread.setName("TelegramPollingThread");
        pollingThread.setDaemon(true);
        pollingThread.start();
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

    private List<Update> getUpdates(long offset) {
        String url = TELEGRAM_API_URL + botToken + "/getUpdates?offset=" + offset + "&timeout=60";
        try {
            TelegramApiResponse apiResponse = restTemplate.getForObject(url, TelegramApiResponse.class);
            return apiResponse != null && apiResponse.isOk() ? apiResponse.getResult() : Collections.emptyList();
        } catch (RestClientException e) {
            log.error("Failed to get updates", e);
            return Collections.emptyList();
        }
    }

    private void processUpdate(Update update) {
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

    private void handlePollingError(Exception e) {
        if (e instanceof HttpClientErrorException.Conflict) {
            log.error("Conflict detected - another bot instance is running. Stopping...");
            running = false;
        } else {
            log.error("Error in polling thread", e);
            try {
                Thread.sleep(RETRY_DELAY_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void fetchBotInfo() {
        String url = TELEGRAM_API_URL + botToken + "/getMe";
        try {
            TelegramApiUserResponse response = restTemplate.getForObject(url, TelegramApiUserResponse.class);
            if (response != null && response.isOk()) {
                this.botId = response.getResult().getId();
                log.info("Bot info fetched. ID: {}, Username: {}", botId, botUsername);
            }
        } catch (RestClientException e) {
            log.error("Failed to fetch bot info", e);
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

        String url = TELEGRAM_API_URL + botToken + "/sendMessage";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> request = new HashMap<>();
        request.put("chat_id", telegramChatId);
        request.put("text", text);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            restTemplate.postForObject(url, entity, String.class);
        } catch (RestClientException e) {
            throw new RuntimeException("Failed to send Telegram message", e);
        }
    }

    /**
     * Отправляет фото в указанный чат Telegram.
     * @param chatId ID чата Telegram (Long).
     * @param photoUrl путь к изображению.
     */
    public void sendPhoto(Long chatId, String photoUrl, String caption) {
        String url = TELEGRAM_API_URL + botToken + "/sendPhoto";

        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("chat_id", chatId.toString());
            body.add("photo", photoUrl);
            body.add("caption", caption);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            restTemplate.postForObject(url, requestEntity, String.class);
        } catch (RestClientException e) {
            log.error("Failed to send photo", e);
        }
    }
}

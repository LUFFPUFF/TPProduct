package com.example.domain.api.chat_service_api.integration.telegram;

import com.example.database.model.company_subscription_module.company.CompanyTelegramConfiguration;
import com.example.database.repository.company_subscription_module.CompanyTelegramConfigurationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpClientErrorException;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;

@Slf4j
public class BotPollingProcess implements Runnable {

    private final CompanyTelegramConfiguration configuration;
    private final BlockingQueue<Object> incomingMessageQueue;
    private final CompanyTelegramConfigurationRepository companyTelegramConfigurationRepository;
    private final TelegramApiClient telegramApiClient;

    private volatile boolean running = true;
    private long offset = 0;
    private static final long RETRY_DELAY_MS = 10000;
    private static final long POLLING_INTERVAL_MS = 100;

    public BotPollingProcess(CompanyTelegramConfiguration configuration,
                             BlockingQueue<Object> incomingMessageQueue,
                             CompanyTelegramConfigurationRepository companyTelegramConfigurationRepository,
                             TelegramApiClient telegramApiClient) {
        this.configuration = Objects.requireNonNull(configuration, "Bot configuration cannot be null");
        this.incomingMessageQueue = Objects.requireNonNull(incomingMessageQueue, "Incoming message queue cannot be null");
        this.companyTelegramConfigurationRepository = Objects.requireNonNull(companyTelegramConfigurationRepository, "CompanyTelegramConfigurationRepository cannot be null");
        this.telegramApiClient = Objects.requireNonNull(telegramApiClient, "TelegramApiClient cannot be null");

        if (configuration.getChatTelegramId() != null) {
            log.info("Starting polling for bot {} with existing chat ID {}", configuration.getBotUsername(), configuration.getChatTelegramId());
        } else {
            log.info("Starting polling for new bot {}", configuration.getBotUsername());
        }
    }

    public void stop() {
        running = false;
        log.info("Stopping polling for bot {}", configuration.getBotUsername());
    }

    @Override
    public void run() {
        Long botId = null;
        Optional<User> botInfo = telegramApiClient.getMe(configuration.getBotToken());
        if (botInfo.isPresent()) {
            botId = botInfo.get().getId();
            log.info("Fetched bot ID {} for username {}", botId, configuration.getBotUsername());
        } else {
            log.error("Failed to fetch bot info for username {}. Polling will not start.", configuration.getBotUsername());
            running = false;
        }

        while (running) {
            try {
                List<Update> updates = telegramApiClient.getUpdates(configuration.getBotToken(), offset);
                for (Update update : updates) {
                    processUpdate(update, botId, configuration.getBotUsername());
                    offset = update.getUpdateId() + 1;
                }
                Thread.sleep(POLLING_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Polling thread interrupted for bot {}", configuration.getBotUsername());
                running = false;
            } catch (HttpClientErrorException.Conflict e) {
                log.error("Conflict detected for bot {} - another bot instance is running? Stopping...", configuration.getBotUsername(), e);
                running = false;
            } catch (Exception e) {
                log.error("Error in polling thread for bot {}", configuration.getBotUsername(), e);
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.info("Polling thread interrupted during retry delay for bot {}", configuration.getBotUsername());
                    running = false;
                }
            }
        }
        log.info("Polling thread finished for bot {}", configuration.getBotUsername());
    }

    private void processUpdate(Update update, Long botId, String botUsername) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            log.debug("Skipping update {} for bot {} (no text message)", update.getUpdateId(), botUsername);
            return;
        }

        Message message = update.getMessage();
        User user = message.getFrom();

        if (user == null || user.getIsBot()) {
            log.debug("Skipping update {} for bot {} (from null or bot user)", update.getUpdateId(), botUsername);
            return;
        }

        log.debug("Processing update {} from user {} for bot {}", update.getUpdateId(), user.getUserName(), botUsername);

        try {
            TelegramResponse response = TelegramResponse.builder()
                    .botId(botId)
                    .botUsername(botUsername)
                    .username(user.getUserName())
                    .firstUsername(user.getFirstName())
                    .text(message.getText())
                    .date(message.getDate())
                    .chatId(message.getChatId())
                    .build();

            updateChatConfiguration(message.getChatId());

            incomingMessageQueue.put(response);
            log.debug("Put TelegramResponse for update {} from bot {} into queue.", update.getUpdateId(), botUsername);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Message processing interrupted for bot {}", botUsername);
        } catch (Exception e) {
            log.error("Error processing Telegram message for bot {}", botUsername, e);
        }
    }

    private void updateChatConfiguration(Long chatId) {
        if (!Objects.equals(configuration.getChatTelegramId(), chatId)) {
            log.info("Updating chat ID for bot {} (company ID {}) from {} to {}",
                    configuration.getBotUsername(), configuration.getCompany().getId(),
                    configuration.getChatTelegramId(), chatId);
            configuration.setChatTelegramId(chatId);
            companyTelegramConfigurationRepository.save(configuration);
            log.info("Updated chat ID for bot {} saved.", configuration.getBotUsername());
        }
    }
}

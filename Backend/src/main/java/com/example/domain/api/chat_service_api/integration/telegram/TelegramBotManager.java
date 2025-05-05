package com.example.domain.api.chat_service_api.integration.telegram;

import com.example.database.model.company_subscription_module.company.CompanyTelegramConfiguration;
import com.example.database.repository.company_subscription_module.CompanyTelegramConfigurationRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramBotManager {

    private final CompanyTelegramConfigurationRepository companyTelegramConfigurationRepository;
    private final BlockingQueue<Object> incomingMessageQueue;
    private final TelegramApiClient telegramApiClient;

    private final Map<Integer, BotPollingProcess> runningBots = new ConcurrentHashMap<>();
    private final Map<Integer, Thread> botPollingThreads = new ConcurrentHashMap<>();

    private ExecutorService executorService;

    @PostConstruct
    public void init() {
        int activeBotCount = companyTelegramConfigurationRepository.countByBotTokenIsNotNullAndBotTokenIsNot("");
        int poolSize = Math.max(1, activeBotCount + 5);
        log.info("Creating a thread pool with {} threads for Telegram bot polling.", poolSize);

        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(0);
            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread t = new Thread(r);
                t.setName("telegram-polling-" + counter.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        };

        executorService = Executors.newFixedThreadPool(poolSize, threadFactory);

        List<CompanyTelegramConfiguration> activeConfigs = companyTelegramConfigurationRepository.findAllByBotTokenIsNotNullAndBotTokenIsNot("");

        log.info("Found {} active Telegram bot configurations. Starting polling processes...", activeConfigs.size());

        for (CompanyTelegramConfiguration config : activeConfigs) {
            try {
                startPollingForCompanyInternal(config);
            } catch (Exception e) {
                log.error("Failed to start polling for bot {} (company ID {})", config.getBotUsername(), config.getCompany().getId(), e);
            }
        }
        log.info("TelegramBotManager initialization complete. Active bots: {}", runningBots.size());
    }

    @PreDestroy
    public void destroy() {
        runningBots.values().forEach(BotPollingProcess::stop);

        executorService.shutdownNow();
        try {
            if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                log.warn("Polling threads did not terminate within the timeout.");
            }
        } catch (InterruptedException e) {
            log.warn("Shutdown interrupted.", e);
            Thread.currentThread().interrupt();
        }
        log.info("TelegramBotManager shutdown complete.");
    }

    /**
     * Запускает или обновляет процесс поллинга для указанной компании.
     * Должен вызываться из UI/сервисного слоя при сохранении/обновлении конфигурации бота пользователем.
     * @param companyId ID компании (Integer, согласно сущности).
     */
    public void startOrUpdatePollingForCompany(Integer companyId) {
        log.info("Attempting to start or update polling for company ID {}", companyId);
        companyTelegramConfigurationRepository.findByCompanyIdAndBotTokenIsNotNullAndBotTokenIsNot(companyId, "")
                .ifPresentOrElse(
                        config -> {
                            stopPollingForCompany(companyId);
                            startPollingForCompanyInternal(config);
                            log.info("Polling process started/updated for bot {} (company ID {})", config.getBotUsername(), companyId);
                        },
                        () -> {
                            stopPollingForCompany(companyId);
                            log.warn("No active Telegram configuration found for company ID {}. Ensuring polling is stopped.", companyId);
                        }
                );
    }

    /**
     * Останавливает процесс поллинга для указанной компании.
     * @param companyId ID компании (Integer).
     */
    public void stopPollingForCompany(Integer companyId) {
        BotPollingProcess botProcess = runningBots.remove(companyId);
        Thread botThread = botPollingThreads.remove(companyId);

        if (botProcess != null && botThread != null) {
            log.info("Stopping polling process for company ID {}", companyId);
            botProcess.stop();
            botThread.interrupt();
            try {
                botThread.join(5000);
                log.info("Polling process for company ID {} stopped successfully.", companyId);
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for polling thread for company ID {} to stop.", companyId);
                Thread.currentThread().interrupt();
            }
        } else {
            log.debug("No active polling process found for company ID {} to stop.", companyId);
        }
    }

    /**
     * Отправляет текстовое сообщение, используя бота указанной компании.
     * Вызывается из слушателя исходящей очереди.
     * @param companyId ID компании (Integer), чей бот будет использоваться.
     * @param chatId ID чата Telegram (Long).
     * @param text Сообщение.
     */
    public void sendTelegramMessage(Integer companyId, Long chatId, String text) {
        log.debug("Attempting to send message to chat ID {} for company ID {}", chatId, companyId);
        companyTelegramConfigurationRepository.findByCompanyIdAndBotTokenIsNotNullAndBotTokenIsNot(companyId, "")
                .ifPresentOrElse(
                        config -> {
                            // Используем TelegramApiClient для отправки сообщения
                            telegramApiClient.sendMessage(config.getBotToken(), chatId, text);
                            log.debug("Sent message to chat ID {} using bot for company ID {}", chatId, companyId);
                        },
                        () -> {
                            log.error("Cannot send message to chat ID {}: No active Telegram configuration found for company ID {}", chatId, companyId);
                            // TODO: Возможно, добавить в очередь ошибок SendMessageCommand обратно или в отдельную очередь ошибок
                        }
                );
    }

    /**
     * Отправляет фото, используя бота указанной компании.
     * Вызывается из слушателя исходящей очереди.
     * @param companyId ID компании (Integer), чей бот будет использоваться.
     * @param chatId ID чата Telegram (Long).
     * @param photoUrl URL фото.
     * @param caption Подпись.
     */
    public void sendTelegramPhoto(Integer companyId, Long chatId, String photoUrl, String caption) {
        log.debug("Attempting to send photo to chat ID {} for company ID {}", chatId, companyId);
        companyTelegramConfigurationRepository.findByCompanyIdAndBotTokenIsNotNullAndBotTokenIsNot(companyId, "")
                .ifPresentOrElse(
                        config -> {
                            telegramApiClient.sendPhoto(config.getBotToken(), chatId, photoUrl, caption);
                            log.debug("Sent photo to chat ID {} using bot for company ID {}", chatId, companyId);
                        },
                        () -> {
                            log.error("Cannot send photo to chat ID {}: No active Telegram configuration found for company ID {}", chatId, companyId);
                        }
                );
    }

    private void startPollingForCompanyInternal(CompanyTelegramConfiguration config) {
        if (config == null) {
            log.error("Cannot start polling: Provided configuration is null.");
            return;
        }
        if (config.getCompany() == null || config.getCompany().getId() == null) {
            log.error("Cannot start polling: Configuration for bot {} has no linked company or company ID is null.", config.getBotUsername());
            return;
        }
        if (config.getBotToken() == null || config.getBotToken().trim().isEmpty()) {
            log.error("Cannot start polling: Bot token is missing or empty for company ID {}.", config.getCompany().getId());
            return;
        }
        if (runningBots.containsKey(config.getCompany().getId())) {
            log.warn("Polling process is already running for company ID {}. Skipping start.", config.getCompany().getId());
            return;
        }


        log.info("Starting polling process for bot {} (company ID {})", config.getBotUsername(), config.getCompany().getId());

        BotPollingProcess botProcess = new BotPollingProcess(
                config,
                incomingMessageQueue,
                companyTelegramConfigurationRepository,
                telegramApiClient
        );

        Thread botThread = new Thread(botProcess);
        botThread.setName("telegram-polling-" + config.getCompany().getId());
        botThread.setDaemon(true);

        botThread.start();

        runningBots.put(config.getCompany().getId(), botProcess);
        botPollingThreads.put(config.getCompany().getId(), botThread);

        log.info("Polling process started for bot {} (company ID {}). Thread: {}",
                config.getBotUsername(), config.getCompany().getId(), botThread.getName());
    }
}

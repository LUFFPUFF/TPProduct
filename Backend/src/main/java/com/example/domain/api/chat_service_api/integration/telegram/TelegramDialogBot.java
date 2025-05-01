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
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
@Slf4j
public class TelegramDialogBot extends TelegramLongPollingBot {

    private final BlockingQueue<TelegramResponse> internalIncomingQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Object> incomingMessageQueue;
    private final CompanyTelegramConfigurationRepository companyTelegramConfigurationRepository;

    private final String botUsername;
    private volatile boolean running = true;
    private BotSession botSession;

    public TelegramDialogBot(@Value("${telegram.test_bot.token}") String botToken,
                             BlockingQueue<Object> incomingMessageQueue,
                             CompanyTelegramConfigurationRepository companyTelegramConfigurationRepository,
                             @Value("${telegram.test_bot.username}") String botUsername) {
        super(botToken);
        this.incomingMessageQueue = incomingMessageQueue;
        this.companyTelegramConfigurationRepository = companyTelegramConfigurationRepository;
        this.botUsername = botUsername;
    }

    @PostConstruct
    public void init() {
        startBotWithRetry();
        startInternalQueueProducer();
    }

    @PreDestroy
    public void destroy() {
        running = false;
        if (botSession != null) {
            botSession.stop();
        }
    }

    private void startInternalQueueProducer() {
        new Thread(() -> {
            while (true) {
                try {
                    TelegramResponse response = getResponse();
                    log.debug("Putting Telegram message from internal queue into main incoming queue: {}", response);
                    incomingMessageQueue.put(response);

                } catch (InterruptedException e) {
                    log.error("Telegram message queue producer interrupted:", e);
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Error putting Telegram message into incoming queue: {}", e.getMessage(), e);
                }
            }
        }).start();
        log.info("Telegram message queue producer started.");
    }

    public TelegramResponse getResponse() {
        try {
            return internalIncomingQueue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            String textMessage = message.getText();
            User user = message.getFrom();
            Integer messageDate = message.getDate();
            Long telegramChatId = message.getChatId();


            GetMe getMe = new GetMe();

            try {
                User botInfo = execute(getMe);

                TelegramResponse response = new TelegramResponse(
                        botInfo.getId(), botInfo.getUserName(), user.getUserName(), user.getFirstName(), textMessage, messageDate
                );

                CompanyTelegramConfiguration companyTelegramConfiguration = companyTelegramConfigurationRepository.findByBotUsername(botUsername)
                        .orElseThrow(() -> new ResourceNotFoundException("Not found company telegram configuration for bot: " + botUsername));
                companyTelegramConfiguration.setChatTelegramId(telegramChatId);

                companyTelegramConfigurationRepository.save(companyTelegramConfiguration);

                incomingMessageQueue.put(response);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                log.error("Failed to put Telegram message into queue:", e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Error creating or putting Telegram message into queue: {}", e.getMessage(), e);
            }
        }
    }


    private void startBotWithRetry() {
        new Thread(() -> {
            while (running) {
                try {
                    TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
                    botSession = botsApi.registerBot(this);
                    log.info("Telegram bot successfully started");
                    break;
                } catch (TelegramApiException e) {
                    log.error("Failed to register bot: {}. Retrying in 10 seconds...", e.getMessage());
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }).start();
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
            throw new RuntimeException(e);
        }

    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }
}

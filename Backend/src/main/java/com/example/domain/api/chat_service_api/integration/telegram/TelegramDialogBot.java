package com.example.domain.api.chat_service_api.integration.telegram;

import com.example.domain.api.chat_service_api.integration.process_service.ClientCompanyProcessService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class TelegramDialogBot extends TelegramLongPollingBot {

    private final BlockingQueue<TelegramResponse> messageQueue = new LinkedBlockingQueue<>();
    private final SimpMessagingTemplate messagingTemplate;
    private final ClientCompanyProcessService clientCompanyProcessService;

    private final String botUsername;

    public TelegramDialogBot(@Value("${telegram.bot.token}") String botToken,
                             SimpMessagingTemplate messagingTemplate,
                             ClientCompanyProcessService clientCompanyProcessService,
                             @Value("${telegram.bot.username}") String botUsername) {
        super(botToken);
        this.messagingTemplate = messagingTemplate;
        this.clientCompanyProcessService = clientCompanyProcessService;
        this.botUsername = botUsername;
    }

    @PostConstruct
    public void init() throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(this);
        startListening();
    }

    private void startListening() {
        new Thread(() -> {
            while (true) {
                TelegramResponse response = getResponse();
                messagingTemplate.convertAndSend("/topic/telegram", response);
            }
        }).start();
    }

    public TelegramResponse getResponse() {
        try {
            return messageQueue.take();
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

            GetMe getMe = new GetMe();

            try {
                User botInfo = execute(getMe);

                TelegramResponse response = new TelegramResponse(
                        botInfo.getId(), botInfo.getUserName(), user.getUserName(), user.getFirstName(), textMessage, messageDate
                );

                clientCompanyProcessService.processTelegram(response);

                messagingTemplate.convertAndSend("/topic/telegram", response);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }
}

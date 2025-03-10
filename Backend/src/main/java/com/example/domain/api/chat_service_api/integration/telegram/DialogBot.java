package com.example.domain.api.chat_service_api.integration.telegram;

import lombok.Setter;
import lombok.SneakyThrows;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


@Setter
public class DialogBot extends TelegramLongPollingBot {

    private final BlockingQueue<TelegramResponse> messageQueue = new LinkedBlockingQueue<>();
    private String botUsername;

    public DialogBot(String botToken, String botUsername) {
        super(botToken);
        this.botUsername = botUsername;
    }

    public static void main(String[] args) throws TelegramApiException {
        DialogBot bot = new DialogBot("7520341907:AAFkCZPkx7d676-qwLMuAhfL4jaXWlU_Blg", "dialog_x_qanswer_bot");

        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

        botsApi.registerBot(bot);

        while (true) {
            TelegramResponse response = bot.getResponse();
            System.out.println("Получено сообщение: " + response);
        }

    }

    public TelegramResponse getResponse() {
        try {
            return messageQueue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            String textMessage = message.getText();
            long chatId = message.getChatId();
            User user = message.getFrom();
            Integer messageDate = message.getDate();

            GetMe getMe = new GetMe();

            User botInfo = execute(getMe);

            TelegramResponse response = new TelegramResponse(
                    botInfo.getId(), botInfo.getUserName(), user.getUserName(), user.getFirstName(), textMessage, messageDate
            );
            messageQueue.offer(response);

            SendMessage sendMessage = getResponse(chatId, textMessage);

            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private static SendMessage getResponse(long chatId, String textMessage) {
        //TODO доделать ответы
        SendMessage response = new SendMessage();
        response.setChatId(String.valueOf(chatId));

        if (textMessage != null) {
            String trimmed = textMessage.trim().toLowerCase();

            if (trimmed.equals("привет")) {
                response.setText("Привет, это бот DialogX");
            } else if (trimmed.equals("как сделать бесконечный картридж для пода?")) {
                response.setText("Никак, ммм и попочку видно");
            } else {
                response.setText("Вы сказали: " + textMessage);
            }
        } else {
            response.setText("Вы отправили не текстовое сообщение.");
        }
        return response;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

}

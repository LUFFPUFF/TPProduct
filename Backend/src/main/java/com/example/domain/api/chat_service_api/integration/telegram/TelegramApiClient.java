package com.example.domain.api.chat_service_api.integration.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class TelegramApiClient {

    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot";

    private final RestTemplate restTemplate;

    public List<Update> getUpdates(String botToken, long offset) {
        String url = TELEGRAM_API_URL + botToken + "/getUpdates?offset=" + offset + "&timeout=60";
        try {
            log.debug("Requesting updates for bot with token starting: {}", botToken.substring(0, 5) + "...");
            TelegramApiResponse apiResponse = restTemplate.getForObject(url, TelegramApiResponse.class);
            if (apiResponse != null && apiResponse.isOk()) {
                log.debug("Received {} updates for bot.", apiResponse.getResult().size());
                return apiResponse.getResult();
            } else {
                log.warn("Telegram API returned not OK response for getUpdates (token starting {}): {}", botToken.substring(0, 5) + "...", apiResponse != null ? apiResponse.getResult() : "null response");
                return Collections.emptyList();
            }
        } catch (RestClientException e) {
            log.error("Failed to get updates for bot (token starting {})", botToken.substring(0, 5) + "...", e);
            return Collections.emptyList();
        }
    }

    public Optional<User> getMe(String botToken) {
        String url = TELEGRAM_API_URL + botToken + "/getMe";
        try {
            log.debug("Requesting getMe for bot with token starting: {}", botToken.substring(0, 5) + "...");
            TelegramApiUserResponse response = restTemplate.getForObject(url, TelegramApiUserResponse.class);
            if (response != null && response.isOk()) {
                log.debug("Successfully fetched bot info: ID {}, Username {}", response.getResult().getId(), response.getResult().getUserName());
                return Optional.ofNullable(response.getResult());
            } else {
                log.warn("Telegram API returned not OK response for getMe (token starting {}): {}", botToken.substring(0, 5) + "...", response != null ? response.getResult() : "null response");
                return Optional.empty();
            }
        } catch (RestClientException e) {
            log.error("Failed to fetch bot info for token starting {}", botToken.substring(0, 5) + "...", e);
            return Optional.empty();
        }
    }

    public void sendMessage(String botToken, Long telegramChatId, String text) {
        if (telegramChatId == null || text == null || text.trim().isEmpty()) {
            log.warn("Attempted to send empty or null message/chatId for token starting {}", botToken.substring(0, 5) + "...");
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
            log.debug("Message sent to chat ID {} using token starting {}", telegramChatId, botToken.substring(0, 5) + "...");
        } catch (RestClientException e) {
            log.error("Failed to send Telegram message to chat ID {} using token starting {}", telegramChatId, botToken.substring(0, 5) + "...", e);
        }
    }

    public void sendPhoto(String botToken, Long chatId, String photoUrl, String caption) {
        if (chatId == null || photoUrl == null || photoUrl.trim().isEmpty()) {
            log.warn("Attempted to send photo with empty or null chatId/photoUrl for token starting {}", botToken.substring(0, 5) + "...");
            return;
        }
        String url = TELEGRAM_API_URL + botToken + "/sendPhoto";

        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("chat_id", chatId.toString());
            body.add("photo", photoUrl);
            if (caption != null) {
                body.add("caption", caption);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            restTemplate.postForObject(url, requestEntity, String.class);
            log.debug("Photo sent to chat ID {} using token starting {}", chatId, botToken.substring(0, 5) + "...");
        } catch (RestClientException e) {
            log.error("Failed to send photo to chat ID {} using token starting {}", chatId, botToken.substring(0, 5) + "...", e);
        }
    }

}

package com.example.domain.api.chat_service_api.integration.manager.whats_app.client;

import com.example.domain.api.chat_service_api.exception_handler.WhatsappApiException;
import com.example.domain.api.chat_service_api.integration.manager.whats_app.model.response.WhatsappSendMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WhatsappApiClient {

    private static final String META_GRAPH_API_BASE_URL = "https://graph.facebook.com/v18.0/";

    private final RestTemplate restTemplate;


    /**
     * Отправляет текстовое сообщение через WhatsApp Business Cloud API.
     *
     * @param accessToken Токен доступа WhatsApp Business (получается из Meta Developer Portal).
     * @param phoneNumberId ID номера телефона WABA (числовой).
     * @param toPhoneNumber Номер телефона получателя в международном формате (без +).
     * @param text Текст сообщения.
     */
    public Optional<WhatsappSendMessageResponse> sendMessage(String accessToken,
                                                             Long phoneNumberId,
                                                             String toPhoneNumber,
                                                             String text) {
        String cleanedToPhoneNumber = toPhoneNumber.startsWith("+") ? toPhoneNumber.substring(1) : toPhoneNumber;

        String url = UriComponentsBuilder.fromUriString(META_GRAPH_API_BASE_URL)
                .pathSegment(phoneNumberId.toString(), "messages")
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("messaging_product", "whatsapp");
        requestBody.put("to", cleanedToPhoneNumber);
        requestBody.put("type", "text");
        Map<String, String> textBody = new HashMap<>();
        textBody.put("body", text);
        requestBody.put("text", textBody);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            log.debug("Attempting to send WhatsApp message to phone {} using phone ID {}", cleanedToPhoneNumber, phoneNumberId);

            ResponseEntity<WhatsappSendMessageResponse> responseEntity =
                    restTemplate.exchange(url, HttpMethod.POST, requestEntity, WhatsappSendMessageResponse.class);

            WhatsappSendMessageResponse apiResponse  = responseEntity.getBody();

            return Optional.ofNullable(apiResponse);
        } catch (HttpClientErrorException e) {
            log.error("HttpClientErrorException sending WhatsApp message (phone ID {}) to {}: Status {} Body {}",
                    phoneNumberId, cleanedToPhoneNumber, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new WhatsappApiException("API error sending WhatsApp message to " + cleanedToPhoneNumber, e);
        } catch (RestClientException e) {
            log.error("RestClientException sending WhatsApp message (phone ID {}) to {}",
                    phoneNumberId, cleanedToPhoneNumber, e);
            throw new WhatsappApiException("RestClientException sending WhatsApp message to " + cleanedToPhoneNumber, e);
        } catch (Exception e) {
            log.error("Unexpected error sending WhatsApp message (phone ID {}) to {}",
                    phoneNumberId, cleanedToPhoneNumber, e);
            throw new WhatsappApiException("Unexpected error sending WhatsApp message to " + cleanedToPhoneNumber, e);
        }
    }

    public boolean verifyTokenAndPhoneNumberId(String accessToken, String phoneNumberId) {
        if (accessToken == null || accessToken.trim().isEmpty() || phoneNumberId == null) {
            return false;
        }

        String url = UriComponentsBuilder.fromUriString(META_GRAPH_API_BASE_URL)
                .pathSegment(phoneNumberId)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            log.debug("Attempting to verify token and phone ID {}...", phoneNumberId);

            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);

            boolean success = responseEntity.getStatusCode().is2xxSuccessful();

            if (success) {
                log.info("Token and phone ID {} verification successful.", phoneNumberId);
            } else {
                log.warn("Token and phone ID {} verification failed with status: {}", phoneNumberId, responseEntity.getStatusCode());
            }
            return success;
        } catch (HttpClientErrorException e) {
            log.warn("Token and phone ID {} verification failed: Status {} Body {}",
                    phoneNumberId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return false;
        }
        catch (RestClientException e) {
            log.error("RestClientException during token and phone ID {} verification", phoneNumberId, e);
            return false;
        }
    }
}

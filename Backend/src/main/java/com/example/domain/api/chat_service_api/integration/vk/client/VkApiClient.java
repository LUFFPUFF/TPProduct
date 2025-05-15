package com.example.domain.api.chat_service_api.integration.vk.client;

import com.example.domain.api.chat_service_api.exception_handler.VkLongPollFailedException;
import com.example.domain.api.chat_service_api.exception_handler.VkApiException;
import com.example.domain.api.chat_service_api.integration.vk.reponse.VkApiResponse;
import com.example.domain.api.chat_service_api.integration.vk.reponse.VkLongPollServerResponse;
import com.example.domain.api.chat_service_api.integration.vk.reponse.VkLongPollUpdatesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class VkApiClient {

    private static final String VK_API_BASE_URL = "https://api.vk.com/method/";
    private static final String API_VERSION = "5.199";
    private static final Random RANDOM = new Random();

    private final RestTemplate restTemplate;

    public VkLongPollServerResponse getCommunityLongPollServer(String accessToken, Long communityId) {
        String url = UriComponentsBuilder.fromUriString(VK_API_BASE_URL)
                .path("groups.getLongPollServer")
                .queryParam("group_id", communityId)
                .queryParam("access_token", accessToken)
                .queryParam("v", API_VERSION)
                .toUriString();

        try {
            log.debug("Requesting Long Poll server for community ID {}", communityId);
            ResponseEntity<VkApiResponse<VkLongPollServerResponse>> responseEntity =
                    restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {
                    });

            VkApiResponse<VkLongPollServerResponse> apiResponse = responseEntity.getBody();

            if (apiResponse != null && apiResponse.isOk()) {
                return apiResponse.getResponse();
            } else {
                log.error("VK API returned error getting Long Poll server for community {}: {}",
                        communityId, apiResponse != null ? apiResponse.getError() : "null response");
                throw new VkApiException("Failed to get Long Poll server for community " + communityId + ": " + (apiResponse != null ? apiResponse.getError().getErrorMsg() : "null response"));
            }
        } catch (RestClientException e) {
            log.error("RestClientException getting Long Poll server for community {}", communityId, e);
            throw new VkApiException("RestClientException getting Long Poll server for community " + communityId, e);
        }
    }

    public VkLongPollUpdatesResponse getLongPollUpdates(String serverUrl, String key, String ts) {
        String url = UriComponentsBuilder.fromUriString(serverUrl)
                .queryParam("act", "a_check")
                .queryParam("key", key)
                .queryParam("ts", ts)
                .queryParam("wait", 25)
                .queryParam("mode", 2)
                .queryParam("v", API_VERSION)
                .toUriString();

        try {
            log.trace("Polling VK Long Poll server with ts={}", ts);
            ResponseEntity<VkLongPollUpdatesResponse> responseEntity =
                    restTemplate.getForEntity(url, VkLongPollUpdatesResponse.class);

            VkLongPollUpdatesResponse updatesResponse = responseEntity.getBody();

            if (updatesResponse != null) {
                return updatesResponse;
            } else {
                log.trace("VK Long Poll server returned null response (likely timeout or no updates)");
                return new VkLongPollUpdatesResponse();
            }
        } catch (RestClientException e) {
            log.error("RestClientException polling VK Long Poll server with ts={}", ts, e);
            try { TimeUnit.SECONDS.sleep(5); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            throw new VkApiException("RestClientException polling Long Poll server", e);
        } catch (VkLongPollFailedException e) {
            log.warn("VK Long Poll failed with code {}", e.getFailedCode());
            throw e;
        }
    }

    public void sendMessage(String accessToken, Long peerId, String message) {
        if (peerId == null || message == null || message.trim().isEmpty()) {
            log.warn("Attempted to send empty or null message/peerId for token starting {}", accessToken.substring(0, 5) + "...");
            return;
        }

        String url = UriComponentsBuilder.fromUriString(VK_API_BASE_URL)
                .path("messages.send")
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("peer_id", peerId.toString());
        requestBody.add("message", message);
        requestBody.add("random_id", RANDOM.nextInt());
        requestBody.add("access_token", accessToken);
        requestBody.add("v", API_VERSION);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            log.debug("Attempting to send VK message to peer ID {} using token starting {}", peerId, accessToken.substring(0, 5) + "...");

            ResponseEntity<VkApiResponse<Object>> responseEntity =
                    restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<>() {});

            VkApiResponse<Object> apiResponse = responseEntity.getBody();

            if (apiResponse != null && apiResponse.isOk()) {
                log.debug("Message sent successfully to peer ID {} using token starting {}. Response: {}",
                        peerId, accessToken.substring(0, 5) + "...", apiResponse.getResponse());
            } else {
                log.error("VK API returned error sending message to peer ID {} using token starting {}: {}",
                        peerId, accessToken.substring(0, 5) + "...", apiResponse != null ? apiResponse.getError() : "null response");

            }
        } catch (RestClientException e) {
            log.error("RestClientException sending VK message to peer ID {} using token starting {}",
                    peerId, accessToken.substring(0, 5) + "...", e);
        }
    }
}

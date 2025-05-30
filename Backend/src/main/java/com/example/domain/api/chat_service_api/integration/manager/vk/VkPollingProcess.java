package com.example.domain.api.chat_service_api.integration.manager.vk;

import com.example.database.model.company_subscription_module.company.CompanyVkConfiguration;
import com.example.domain.api.chat_service_api.exception_handler.VkApiException;
import com.example.domain.api.chat_service_api.exception_handler.VkLongPollFailedException;
import com.example.domain.api.chat_service_api.integration.manager.vk.client.VkApiClient;
import com.example.domain.api.chat_service_api.integration.manager.vk.model.VkLongPollUpdate;
import com.example.domain.api.chat_service_api.integration.manager.vk.model.VkMessage;
import com.example.domain.api.chat_service_api.integration.manager.vk.reponse.VkLongPollServerResponse;
import com.example.domain.api.chat_service_api.integration.manager.vk.reponse.VkLongPollUpdatesResponse;
import com.example.domain.api.chat_service_api.integration.manager.vk.reponse.VkResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class VkPollingProcess implements Runnable {

    private volatile boolean running = true;

    private final CompanyVkConfiguration configuration;
    private final BlockingQueue<Object> incomingMessageQueue;
    private final VkApiClient vkApiClient;

    private String serverUrl;
    private String key;
    private Integer ts;

    @Override
    public void run() {
        log.info("Starting VK polling for community ID {} (company ID {})",
                configuration.getCommunityId(), configuration.getCompany().getId());

        while (running) {
            try {
                if (serverUrl == null || key == null || ts == null) {
                    VkLongPollServerResponse serverConfig = vkApiClient.getCommunityLongPollServer(
                            configuration.getAccessToken(),
                            configuration.getCommunityId()
                    );
                    serverUrl = serverConfig.getServer();
                    key = serverConfig.getKey();
                    ts = Integer.parseInt(serverConfig.getTs());
                    log.info("Obtained new VK Long Poll server config for community {}: server={}, key={}, ts={}",
                            configuration.getCommunityId(), serverUrl, key, ts);
                }

                VkLongPollUpdatesResponse updatesResponse = vkApiClient.getLongPollUpdates(serverUrl, key, ts.toString());

                if (updatesResponse != null) {
                    if (updatesResponse.getTs() != null) {
                        ts = updatesResponse.getTs();
                    }

                    List<VkLongPollUpdate> updates = updatesResponse.getUpdates();
                    if (updates != null && !updates.isEmpty()) {
                        log.debug("Received {} updates for community {}", updates.size(), configuration.getCommunityId());
                        processUpdates(updates);
                    }
                }

                TimeUnit.MILLISECONDS.sleep(50);
            } catch (VkLongPollFailedException e) {
                log.warn("VK Long Poll failed for community {} with code {}. Getting new server config.",
                        configuration.getCommunityId(), e.getFailedCode());
                switch (e.getFailedCode()) {
                    case 1:
                        if (e.getNewTs() != null) {
                            ts = e.getNewTs();
                            log.info("VK Long Poll: Invalid ts, updated to {}", ts);
                        } else {
                            log.warn("VK Long Poll: Invalid ts, getting new server config.");
                            serverUrl = null; key = null; ts = null;
                        }
                        break;
                    case 2: // Invalid key
                    case 3: // Server killed
                        log.warn("VK Long Poll: Invalid key or server killed. Getting new server config.");
                        serverUrl = null; key = null; ts = null;
                        break;
                    default:
                        log.error("VK Long Poll: Unknown failure code {} for community {}. Getting new server config.", e.getFailedCode(), configuration.getCommunityId());
                        serverUrl = null; key = null; ts = null;
                        break;
                }
                try { TimeUnit.SECONDS.sleep(5); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }

            }catch (VkApiException e) {
                log.error("VK API Exception during polling for community {} (company ID {}): {}",
                        configuration.getCommunityId(), configuration.getCompany().getId(), e.getMessage(), e);
                try { TimeUnit.SECONDS.sleep(10); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                serverUrl = null; key = null; ts = null;
            } catch (InterruptedException e) {
                running = false;
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Unexpected error during VK polling for community ID {} (company ID {}): {}",
                        configuration.getCommunityId(), configuration.getCompany().getId(), e.getMessage(), e);
                try { TimeUnit.SECONDS.sleep(15); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                serverUrl = null; key = null; ts = null;
            }
        }
    }

    private void processUpdates(List<VkLongPollUpdate> updates) {
        for (VkLongPollUpdate update : updates) {
            if ("message_new".equals(update.getType()) && update.getObject() != null && update.getObject().getMessage() != null) {
                try {
                    VkMessage vkMessage = update.getObject().getMessage();

                    VkResponse vkResponse = VkResponse.builder()
                            .companyId(configuration.getCompany().getId())
                            .communityId(configuration.getCommunityId())
                            .peerId(vkMessage.getPeerId())
                            .fromId(vkMessage.getFromId())
                            .text(vkMessage.getText())
                            .timestamp(Instant.ofEpochSecond(vkMessage.getDate()))
                            .build();

                    incomingMessageQueue.put(vkResponse);
                    log.debug("Put VK message into incoming queue: peer={}, from={}",
                            vkResponse.getPeerId(), vkResponse.getFromId());
                } catch (InterruptedException e) {
                    log.warn("Interrupted while putting VK message into queue.", e);
                    Thread.currentThread().interrupt();
                    running = false;
                    break;
                } catch (Exception e) {
                    log.error("Error processing VK update for community {}: {}", configuration.getCommunityId(), e.getMessage(), e);
                }
            }
        }
    }

    public void stop() {
        log.info("Stopping VK polling process requested for community ID {} (company ID {})",
                configuration.getCommunityId(), configuration.getCompany().getId());
        this.running = false;
    }
}

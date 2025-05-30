package com.example.domain.api.chat_service_api.integration.manager.vk;

import com.example.database.model.company_subscription_module.company.CompanyVkConfiguration;
import com.example.database.repository.company_subscription_module.CompanyVkConfigurationRepository;
import com.example.domain.api.chat_service_api.integration.manager.vk.client.VkApiClient;
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
public class VkBotManager {

    private final CompanyVkConfigurationRepository companyVkConfigurationRepository;
    private final BlockingQueue<Object> incomingMessageQueue;
    private final VkApiClient vkApiClient;
    private final Map<Integer, VkPollingProcess> runningBots = new ConcurrentHashMap<>();
    private final Map<Integer, Thread> botPollingThreads = new ConcurrentHashMap<>();
    private ExecutorService executorService;

    @PostConstruct
    public void init() {
        int activeBot = companyVkConfigurationRepository.countByAccessTokenIsNotNullAndAccessTokenIsNot("");

        int poolSize = Math.min(1, activeBot + 5);

        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(0);
            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread t = new Thread(r);
                t.setName("vk-polling-" + counter.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        };

        executorService = Executors.newFixedThreadPool(poolSize, threadFactory);

        List<CompanyVkConfiguration> activeConfigs =
                companyVkConfigurationRepository.findAllByAccessTokenIsNotNullAndAccessTokenIsNot("");

        log.info("Found {} active VK bot configurations. Starting polling processes...", activeConfigs.size());

        for (CompanyVkConfiguration config : activeConfigs) {
            try {
                startPollingForCompanyInternal(config);
            } catch (Exception e) {
                log.error("Failed to start polling for VK community {} (company ID {})",
                        config.getCommunityId(), config.getCompany() != null ? config.getCompany().getId() : "N/A", e);
            }
        }
        log.info("VkBotManager initialization complete. Active bots: {}", runningBots.size());
    }

    @PreDestroy
    public void destroy() {
        log.info("Shutting down VkBotManager...");
        runningBots.values().forEach(VkPollingProcess::stop);

        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("VK polling threads did not terminate within the timeout. Forcing shutdown.");

                executorService.shutdownNow();

                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.error("VK polling threads failed to terminate after forced shutdown.");
                }
            }
        } catch (InterruptedException e) {
            log.warn("Shutdown process interrupted.", e);
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }
        log.info("VkBotManager shutdown complete.");
    }

    public void startOrUpdatePollingForCompany(Integer companyId) {
        log.info("Attempting to start or update VK polling for company ID {}", companyId);
        companyVkConfigurationRepository.findByCompanyIdAndAccessTokenIsNotNullAndAccessTokenIsNot(companyId, "")
                .ifPresentOrElse(
                        config -> {
                            stopPollingForCompany(companyId);
                            startPollingForCompanyInternal(config);
                            log.info("VK polling process started/updated for community {} (company ID {})",
                                    config.getCommunityId(), companyId);
                        },
                        () -> {
                            stopPollingForCompany(companyId);
                            log.warn("No active VK configuration found for company ID {}. Ensuring polling is stopped.", companyId);
                        }
                );
    }

    public void stopPollingForCompany(Integer companyId) {
        VkPollingProcess botProcess = runningBots.remove(companyId);
        Thread botThread = botPollingThreads.remove(companyId);

        if (botProcess != null && botThread != null) {
            botProcess.stop();
            botThread.interrupt();
            try {
                botThread.join(5000);
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for VK polling thread for company ID {} to stop.", companyId);
                Thread.currentThread().interrupt();
            }
        } else {
            log.debug("No active VK polling process found for company ID {} to stop.", companyId);
        }
    }

    public void sendVkMessage(Integer companyId, Long peerId, String text) {
        log.debug("Attempting to send VK message to peer ID {} for company ID {}", peerId, companyId);
        companyVkConfigurationRepository.findByCompanyIdAndAccessTokenIsNotNullAndAccessTokenIsNot(companyId, "")
                .ifPresentOrElse(
                        config -> {
                            vkApiClient.sendMessage(config.getAccessToken(), peerId, text);
                            log.debug("Sent VK message to peer ID {} using bot for company ID {}", peerId, companyId);
                        },
                        () -> {
                            log.error("Cannot send VK message to peer ID {}: No active VK configuration found for company ID {}", peerId, companyId);
                        }
                );
    }

    private void startPollingForCompanyInternal(CompanyVkConfiguration config) {
        if (config == null) {
            log.error("Cannot start VK polling: Provided configuration is null.");
            return;
        }
        if (config.getCompany() == null || config.getCompany().getId() == null) {
            log.error("Cannot start VK polling: Configuration for community {} has no linked company or company ID is null.", config.getCommunityId());
            return;
        }
        if (!config.isActive()) {
            log.error("Cannot start VK polling: Configuration for company ID {} is not active (missing token or community ID).", config.getCompany().getId());
            return;
        }
        if (runningBots.containsKey(config.getCompany().getId())) {
            log.warn("VK polling process is already running for company ID {}. Skipping start.", config.getCompany().getId());
            return;
        }

        VkPollingProcess botProcess = new VkPollingProcess(
                config,
                incomingMessageQueue,
                vkApiClient
        );

        Thread botThread = new Thread(botProcess);
        botThread.setName("vk-polling-" + config.getCompany().getId());
        botThread.setDaemon(true);

        botThread.start();

        runningBots.put(config.getCompany().getId(), botProcess);
        botPollingThreads.put(config.getCompany().getId(), botThread);

        log.info("VK polling process started for community {} (company ID {}). Thread: {}",
                config.getCommunityId(), config.getCompany().getId(), botThread.getName());
    }
}

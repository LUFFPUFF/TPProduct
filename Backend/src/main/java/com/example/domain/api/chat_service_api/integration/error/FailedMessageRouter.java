package com.example.domain.api.chat_service_api.integration.error;

import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.domain.api.chat_service_api.integration.exception.ChannelSenderException;
import com.example.domain.api.chat_service_api.integration.listener.model.SendMessageCommand;
import com.example.domain.api.chat_service_api.integration.sender.ChannelSender;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.*;

@Service
@Slf4j
public class FailedMessageRouter {

    @Value("${application.integration.retry.in-memory.max-attempts:3}")
    private int maxAttempts;

    @Value("${application.integration.retry.in-memory.initial-delay-ms:5000}")
    private long initialDelayMs;

    @Value("${application.integration.retry.in-memory.delay-multiplier:2}")
    private double delayMultiplier;

    @Value("${application.integration.retry.in-memory.max-delay-ms:300000}")
    private long maxDelayMs;

    private final DelayQueue<RetriableCommand> retryQueue = new DelayQueue<>();
    private final ScheduledExecutorService retryExecutor;
    private final Map<ChatChannel, ChannelSender> channelSenders;

    public FailedMessageRouter(
            @Qualifier("telegramChannelSender") ChannelSender telegramSender,
            @Qualifier("emailChannelSender") ChannelSender emailSender,
            @Qualifier("vkChannelSender") ChannelSender vkSender,
            @Qualifier("whatsappChannelSender") ChannelSender whatsappSender,
            @Qualifier("dialogXChatChannelSender") ChannelSender dialogXChatSender
    ) {
        this.channelSenders = Map.of(
                ChatChannel.Telegram, telegramSender,
                ChatChannel.Email, emailSender,
                ChatChannel.VK, vkSender,
                ChatChannel.WhatsApp, whatsappSender,
                ChatChannel.DialogX_Chat, dialogXChatSender
        );
        this.retryExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("failed-message-retry-executor");
            t.setDaemon(true);
            return t;
        });
    }

    @PostConstruct
    public void init() {
        log.info("FailedMessageRouter initialized with maxAttempts={}, initialDelayMs={}, multiplier={}",
                maxAttempts, initialDelayMs, delayMultiplier);
        retryExecutor.scheduleWithFixedDelay(this::processRetryQueue, initialDelayMs, initialDelayMs / 2, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down FailedMessageRouter retry executor...");
        retryExecutor.shutdown();
        try {
            if (!retryExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                retryExecutor.shutdownNow();
                if (!retryExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.error("Retry executor did not terminate.");
                }
            }
        } catch (InterruptedException ie) {
            retryExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("FailedMessageRouter retry executor shut down. {} items remaining in retry queue.", retryQueue.size());
    }

    public void routeFailedCommand(SendMessageCommand command, String reason, Throwable cause) {
        log.warn("Failed to send command. Reason: [{}]. Command: [{}]. Cause: [{}]",
                reason, command, cause != null ? cause.getMessage() : "N/A", cause);

        RetriableCommand retriableCommand = new RetriableCommand(command, reason, cause, 0, System.currentTimeMillis() + initialDelayMs);

        if (retriableCommand.attemptCount() < maxAttempts) {
            boolean offered = retryQueue.offer(retriableCommand);
            if (offered) {
                log.info("Scheduled command for retry (attempt {}/{}). Chat ID: {}. Next attempt in {} ms. Queue size: {}",
                        retriableCommand.attemptCount() + 1, maxAttempts, command.getChatId(), initialDelayMs, retryQueue.size());
            } else {
                log.error("CRITICAL: Failed to offer command to retryQueue! Queue might be full or malfunctioning. Command: {}", command);
                handleUnrecoverableFailure(command, "Failed to add to retry queue", cause);
            }
        } else {
            log.error("Command reached max retry attempts ({}) and will not be retried further. Command: {}. Reason: {}",
                    maxAttempts, command, reason);
            handleUnrecoverableFailure(command, reason, cause);
        }
    }

    private void processRetryQueue() {
        RetriableCommand commandToRetry;
        while ((commandToRetry = retryQueue.poll()) != null) {
            SendMessageCommand originalCommand = commandToRetry.command();
            log.info("Retrying command (attempt {}/{}) for chat ID {} on channel {}",
                    commandToRetry.attemptCount() + 1, maxAttempts,
                    originalCommand.getChatId(), originalCommand.getChannel());

            ChannelSender sender = channelSenders.get(originalCommand.getChannel());
            if (sender == null) {
                log.error("No sender configured for channel: {} during retry. Command: {}", originalCommand.getChannel(), originalCommand);
                handleUnrecoverableFailure(originalCommand, "No sender for channel during retry", null);
                continue;
            }

            try {
                sender.send(originalCommand);
                log.info("Successfully resent command after {} attempts. Chat ID: {}",
                        commandToRetry.attemptCount() + 1, originalCommand.getChatId());
            } catch (ChannelSenderException e) {
                log.warn("Retry attempt {}/{} failed for command. Chat ID: {}. Reason: {}. Cause: {}",
                        commandToRetry.attemptCount() + 1, maxAttempts,
                        originalCommand.getChatId(), e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "N/A");

                int nextAttemptCount = commandToRetry.attemptCount() + 1;
                if (nextAttemptCount < maxAttempts) {
                    long nextDelay = calculateNextDelay(commandToRetry.attemptCount());
                    RetriableCommand nextRetry = new RetriableCommand(
                            originalCommand,
                            commandToRetry.initialReason(),
                            e,
                            nextAttemptCount,
                            System.currentTimeMillis() + nextDelay
                    );
                    retryQueue.offer(nextRetry);
                    log.info("Re-scheduled command for retry (attempt {}/{}). Chat ID: {}. Next attempt in {} ms. Queue size: {}",
                            nextAttemptCount + 1, maxAttempts, originalCommand.getChatId(), nextDelay, retryQueue.size());
                } else {
                    log.error("Command reached max retry attempts ({}) after retry failure. Chat ID: {}. Final Reason: {}",
                            maxAttempts, originalCommand.getChatId(), e.getMessage());
                    handleUnrecoverableFailure(originalCommand, "Max retries reached", e);
                }
            } catch (Exception ex) {
                log.error("Unexpected error during retry of command {}: {}", originalCommand, ex.getMessage(), ex);
                handleUnrecoverableFailure(originalCommand, "Unexpected error during retry", ex);
            }
        }
    }

    private long calculateNextDelay(int currentAttempts) {
        long delay = (long) (initialDelayMs * Math.pow(delayMultiplier, currentAttempts));
        return Math.min(delay, maxDelayMs);
    }

    private void handleUnrecoverableFailure(SendMessageCommand command, String reason, Throwable cause) {
        log.error("UNRECOVERABLE FAILURE for command: {}. Reason: {}. Cause: {}",
                command, reason, cause != null ? cause.getMessage() : "N/A", cause);
    }

    public void routeUnknownObject(Object unknownObject, String reason, Class<?> expectedType) {
        String objectDetails = unknownObject != null ? unknownObject.toString() : "null";
        objectDetails = objectDetails.substring(0, Math.min(objectDetails.length(), 1000)) + (objectDetails.length() > 1000 ? "..." : "");

        log.error("Received unknown object. Reason: [{}]. Expected type: [{}]. Actual object details: [{}]",
                reason, expectedType.getName(), objectDetails);
    }

    private record RetriableCommand(SendMessageCommand command, String initialReason, Throwable lastCause,
                                        int attemptCount, long executeAtTime) implements Delayed {

        @Override
        public long getDelay(TimeUnit unit) {
            long diff = executeAtTime - System.currentTimeMillis();
            return unit.convert(diff, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(@NotNull Delayed other) {
            return Long.compare(this.executeAtTime, ((RetriableCommand) other).executeAtTime);
        }

        @Override
        public String toString() {
            return "RetriableCommand{" +
                    "command=" + command.getChatId() + " ch:" + command.getChannel() +
                    ", attempt=" + attemptCount +
                    ", executeAt=" + executeAtTime +
                    '}';
        }
    }
}

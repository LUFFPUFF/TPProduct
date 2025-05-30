package com.example.domain.api.chat_service_api.integration.manager.widget;

import com.example.database.model.company_subscription_module.company.CompanyDialogXChatConfiguration;
import com.example.database.repository.company_subscription_module.CompanyDialogXChatConfigurationRepository;
import com.example.domain.api.chat_service_api.config.WebSocketTopicRegistry;
import com.example.domain.api.chat_service_api.integration.dto.DialogXChatOutgoingMessage;
import com.example.domain.api.chat_service_api.service.WebSocketMessagingService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DialogXChatManager {

    private final CompanyDialogXChatConfigurationRepository configurationRepository;
    private final Set<Integer> activeWidgetCompanyIds = ConcurrentHashMap.newKeySet();
    private final WebSocketMessagingService wsMessagingService;
    private final WebSocketTopicRegistry topicRegistry;

    @PostConstruct
    public void init() {
        log.info("Initializing DialogXChatManager...");
        List<CompanyDialogXChatConfiguration> activeConfigs = configurationRepository.findAllByEnabledTrue();
        for (CompanyDialogXChatConfiguration config : activeConfigs) {
            if (config.getCompany() != null && config.getCompany().getId() != null) {
                activeWidgetCompanyIds.add(config.getCompany().getId());
                log.info("DialogX Chat widget initially active for company ID: {}", config.getCompany().getId());
            }
        }
        log.info("DialogXChatManager initialized. {} DialogX Chat widgets active.", activeWidgetCompanyIds.size());
    }

    public void processConfigurationChange(Integer companyId) {
        if (companyId == null) {
            log.warn("processConfigurationChange called with null companyId.");
            return;
        }

        configurationRepository.findByCompanyId(companyId).ifPresentOrElse(
                config -> {
                    if (config.isEnabled()) {
                        if (activeWidgetCompanyIds.add(companyId)) {
                            log.info("DialogX Chat widget activated for company ID: {}", companyId);
                        } else {
                            log.info("DialogX Chat widget remains active for company ID: {}", companyId);
                        }
                    } else {
                        if (activeWidgetCompanyIds.remove(companyId)) {
                            log.info("DialogX Chat widget deactivated for company ID: {}", companyId);
                        } else {
                            log.info("DialogX Chat widget remains inactive for company ID: {}", companyId);
                        }
                    }
                },
                () -> {
                    if (activeWidgetCompanyIds.remove(companyId)) {
                        log.info("DialogX Chat widget configuration removed, deactivated for company ID: {}", companyId);
                    }
                }
        );
    }

    /**
     * Отправляет текстовое сообщение конкретной сессии виджета через WebSocket.
     * Предполагается, что clientSessionId был установлен как Principal для STOMP сессии виджета.
     *
     * @param companyId       ID компании, к которой привязан виджет.
     * @param clientSessionId Уникальный ID сессии виджета (должен быть principal.getName()).
     * @param text            Текст сообщения.
     * @param senderName      Имя отправителя (например, "Оператор", "Бот").
     */
    public void sendMessageToWidget(Integer companyId, String clientSessionId, String text, String senderName) {
        if (companyId == null || clientSessionId == null || clientSessionId.isBlank() || text == null) {
            log.warn("sendMessageToWidget called with invalid parameters. companyId={}, clientSessionId='{}', textPresent={}",
                    companyId, clientSessionId, text != null);
            return;
        }

        if (!isWidgetActiveForCompany(companyId)) {
            log.warn("DialogX Chat widget is not active for company ID {}. Cannot send message to client session {}.",
                    companyId, clientSessionId);
            return;
        }

        log.info("Attempting to send message to DialogX widget client session: {}, CompanyID={}, Text='{}...'",
                clientSessionId, companyId, text.substring(0, Math.min(text.length(), 50)));

        DialogXChatOutgoingMessage outgoingMessage = new DialogXChatOutgoingMessage(
                "message",
                text,
                senderName != null ? senderName.trim() : "Поддержка",
                Instant.now().toEpochMilli()
        );

        String destinationSuffix = topicRegistry.getWidgetSessionMessagesDestinationSuffix();

        try {
            wsMessagingService.sendToUser(clientSessionId, destinationSuffix, outgoingMessage);

            log.info("Message for widget client session {} (company {}) sent via sendToUser. Destination Suffix: '{}'. Payload Type: {}",
                    clientSessionId, companyId, destinationSuffix, outgoingMessage.getType());
        } catch (Exception e) {
            log.error("Failed to send message to widget client session {} (company {}): {}",
                    clientSessionId, companyId, e.getMessage(), e);
        }

    }

    /**
     * Отправляет обновление конфигурации конкретной сессии виджета.
     *
     * @param clientSessionId Уникальный ID сессии виджета.
     * @param configPayload   Объект с данными конфигурации для отправки (например, обновленный DialogXChatDto или специальный DTO).
     */
    public void sendConfigUpdateToWidget(String clientSessionId, Object configPayload) {
        if (clientSessionId == null || clientSessionId.isBlank() || configPayload == null) {
            log.warn("sendConfigUpdateToWidget called with invalid parameters.");
            return;
        }
        log.info("Attempting to send config update to DialogX widget client session: {}", clientSessionId);

        String destinationSuffix = topicRegistry.getWidgetSessionConfigDestinationSuffix();
        try {
            wsMessagingService.sendToUser(clientSessionId, destinationSuffix, configPayload);
            log.info("Config update for widget client session {} sent. Destination Suffix: '{}'.", clientSessionId, destinationSuffix);
        } catch (Exception e) {
            log.error("Failed to send config update to widget client session {}: {}", clientSessionId, e.getMessage(), e);
        }
    }

    /**
     * Отправляет системное уведомление конкретной сессии виджета.
     *
     * @param clientSessionId Уникальный ID сессии виджета.
     * @param notificationText Текст уведомления.
     */
    public void sendSystemNotificationToWidget(String clientSessionId, String notificationText) {
        if (clientSessionId == null || clientSessionId.isBlank() || notificationText == null || notificationText.isBlank()) {
            log.warn("sendSystemNotificationToWidget called with invalid parameters.");
            return;
        }
        log.info("Attempting to send system notification to DialogX widget client session: {}. Text: '{}'", clientSessionId, notificationText);

        // Используем DialogXChatOutgoingMessage с типом "system_notification"
        DialogXChatOutgoingMessage notificationPayload = new DialogXChatOutgoingMessage(
                "system_notification",
                notificationText,
                "Система",
                Instant.now().toEpochMilli()
        );

        String destinationSuffix = topicRegistry.getWidgetSessionNotificationsDestinationSuffix();
        try {
            wsMessagingService.sendToUser(clientSessionId, destinationSuffix, notificationPayload);
            log.info("System notification for widget client session {} sent. Destination Suffix: '{}'.", clientSessionId, destinationSuffix);
        } catch (Exception e) {
            log.error("Failed to send system notification to widget client session {}: {}", clientSessionId, e.getMessage(), e);
        }
    }

    public boolean isWidgetActiveForCompany(Integer companyId) {
        return companyId != null && activeWidgetCompanyIds.contains(companyId);
    }
}

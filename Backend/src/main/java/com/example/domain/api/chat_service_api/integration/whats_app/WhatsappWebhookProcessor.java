package com.example.domain.api.chat_service_api.integration.whats_app;

import com.example.database.model.company_subscription_module.company.CompanyWhatsappConfiguration;
import com.example.database.repository.company_subscription_module.CompanyWhatsappConfigurationRepository;
import com.example.domain.api.chat_service_api.integration.whats_app.model.WhatsappWebhookChange;
import com.example.domain.api.chat_service_api.integration.whats_app.model.WhatsappWebhookEntry;
import com.example.domain.api.chat_service_api.integration.whats_app.model.WhatsappWebhookMessage;
import com.example.domain.api.chat_service_api.integration.whats_app.model.WhatsappWebhookValue;
import com.example.domain.api.chat_service_api.integration.whats_app.model.request.WhatsappWebhookRequest;
import com.example.domain.api.chat_service_api.integration.whats_app.model.response.WhatsappResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.BlockingQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsappWebhookProcessor {

    private final BlockingQueue<Object> incomingMessageQueue;
    private final CompanyWhatsappConfigurationRepository whatsappConfigurationRepository;

    public void process(WhatsappWebhookRequest request) {
        if (request == null || !"whatsapp_business_account".equals(request.getObject()) || request.getEntry() == null) {
            log.warn("Invalid webhook request received. Object: {}", request != null ? request.getObject() : "null");
            return;
        }

        for (WhatsappWebhookEntry entry : request.getEntry()) {
            if (entry.getChanges() != null) {
                for (WhatsappWebhookChange change : entry.getChanges()) {
                    if ("messages".equals(change.getField()) && change.getValue() != null) {
                        WhatsappWebhookValue value = change.getValue();

                        if (value.getMessages() != null) {
                            for (WhatsappWebhookMessage message : value.getMessages()) {
                                Long recipientPhoneNumberId = value.getMetadata() != null ? value.getMetadata().getPhoneNumberId() : null;

                                if (recipientPhoneNumberId == null) {
                                    log.error("Webhook event message missing recipient phone number ID in metadata. Cannot process.");
                                    continue;
                                }

                                CompanyWhatsappConfiguration config = whatsappConfigurationRepository.findByPhoneNumberId(recipientPhoneNumberId)
                                        .orElseGet(() -> {
                                            log.error("No WhatsApp configuration found for phone number ID {}. Cannot process message {} from {}",
                                                    recipientPhoneNumberId, message.getId(), message.getFrom());
                                            return null;
                                        });

                                if (config == null) {
                                    continue;
                                }

                                if ("text".equals(message.getType()) && message.getText() != null) {
                                    log.debug("Processing new WhatsApp text message from {} to phone ID {}: {}",
                                            message.getFrom(), recipientPhoneNumberId, message.getText().getBody());

                                    WhatsappResponse whatsappResponse = WhatsappResponse.builder()
                                            .companyId(config.getCompany().getId())
                                            .recipientPhoneNumberId(recipientPhoneNumberId)
                                            .fromPhoneNumber(message.getFrom())
                                            .text(message.getText().getBody())
                                            .messageId(message.getId())
                                            .timestamp(Instant.ofEpochSecond(message.getTimestamp()))
                                            .build();

                                    try {
                                        incomingMessageQueue.put(whatsappResponse);
                                        log.debug("Put WhatsApp message into incoming queue: {}", message.getId());
                                    } catch (InterruptedException e) {
                                        log.error("Failed to put WhatsApp message into incoming queue: {}", message.getId(), e);
                                        Thread.currentThread().interrupt();
                                        return;
                                    }

                                }
                                // TODO: Добавить обработку других типов сообщений (image, location, etc.)

                            }
                        }
                    }
                }
            }
        }
    }
}

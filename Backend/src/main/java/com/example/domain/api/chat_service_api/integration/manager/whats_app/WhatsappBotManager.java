package com.example.domain.api.chat_service_api.integration.manager.whats_app;

import com.example.database.model.company_subscription_module.company.CompanyWhatsappConfiguration;
import com.example.database.repository.company_subscription_module.CompanyWhatsappConfigurationRepository;
import com.example.domain.api.chat_service_api.integration.manager.whats_app.client.WhatsappApiClient;
import com.example.domain.api.chat_service_api.integration.manager.whats_app.model.response.WhatsappSendMessageResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsappBotManager {

    private final CompanyWhatsappConfigurationRepository companyWhatsappConfigurationRepository;
    private final WhatsappApiClient whatsappApiClient;

    @PostConstruct
    public void init() {
        log.info("WhatsappBotManager initialized.");
    }

    /**
     * Отправляет текстовое сообщение через WhatsApp Business Cloud API.
     * Вызывается из слушателя исходящей очереди.
     *
     * @param companyId ID компании, чей аккаунт будет использоваться.
     * @param recipientPhoneNumber Номер телефона получателя в международном формате.
     * @param text Сообщение.
     * @return Optional<String> - Optional с ID отправленного сообщения, если успешно.
     */
    public Optional<String> sendWhatsappMessage(Integer companyId, String recipientPhoneNumber, String text) {
        log.debug("Attempting to send WhatsApp message to phone {} for company ID {}", recipientPhoneNumber, companyId);

        Optional<CompanyWhatsappConfiguration> configOptional = companyWhatsappConfigurationRepository
                .findByCompanyIdAndAccessTokenIsNotNullAndAccessTokenIsNot(companyId, "");

        if (configOptional.isPresent()) {
            CompanyWhatsappConfiguration config = configOptional.get();

            Optional<WhatsappSendMessageResponse> sendResponse =
                    whatsappApiClient.sendMessage(config.getAccessToken(), config.getPhoneNumberId(), recipientPhoneNumber, text);

            if (sendResponse.isPresent() && sendResponse.get().getMessages() != null && !sendResponse.get().getMessages().isEmpty()) {
                String messageId = sendResponse.get().getMessages().get(0).getId();
                return Optional.of(messageId);
            } else {
                log.error("Failed to send WhatsApp message to phone {} using account for company ID {}", recipientPhoneNumber, companyId);
                return Optional.empty();
            }

        } else {
            log.error("Cannot send WhatsApp message to phone {}: No active WhatsApp configuration found for company ID {}", recipientPhoneNumber, companyId);
            return Optional.empty();
        }
    }
}

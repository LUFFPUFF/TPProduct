package com.example.domain.api.chat_service_api.integration.service.impl;

import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.company_subscription_module.company.*;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.integration.dto.DialogXChatIncomingMessage;
import com.example.domain.api.chat_service_api.integration.dto.IncomingChannelMessage;
import com.example.domain.api.chat_service_api.integration.manager.mail.response.EmailResponse;
import com.example.domain.api.chat_service_api.integration.service.ICompanyChannelConfigurationProvider;
import com.example.domain.api.chat_service_api.integration.service.IIncomingMessageProcessorService;
import com.example.domain.api.chat_service_api.integration.manager.telegram.TelegramResponse;
import com.example.domain.api.chat_service_api.integration.manager.vk.reponse.VkResponse;
import com.example.domain.api.chat_service_api.integration.manager.whats_app.model.response.WhatsappResponse;
import com.example.domain.api.statistics_module.metrics.service.IChatMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientCompanyProcessService {

    private final ICompanyChannelConfigurationProvider configProvider;
    private final IIncomingMessageProcessorService messageProcessorService;
    private final IChatMetricsService chatMetricsService;

    @Transactional
    public void processTelegram(TelegramResponse telegramResponse) {
        log.info("Processing incoming Telegram message from bot: {}, user: {}", telegramResponse.getBotUsername(), telegramResponse.getUsername());
        Company company = configProvider.findCompanyByChannelIdentifier(telegramResponse.getBotUsername(), ChatChannel.Telegram)
                .orElseThrow(() -> {
                    log.error("Telegram configuration not found for bot: {}", telegramResponse.getBotUsername());
                    chatMetricsService.incrementChatOperationError(
                            "ProcessTelegram.ConfigNotFound",
                            "unknown_company_tg_bot_" + telegramResponse.getBotUsername(),
                            ResourceNotFoundException.class.getSimpleName()
                    );
                    return new ResourceNotFoundException("Telegram configuration not found for bot: " + telegramResponse.getBotUsername());
                });

        IncomingChannelMessage message = IncomingChannelMessage.builder()
                .channelSpecificUserId(telegramResponse.getUsername())
                .messageContent(telegramResponse.getText())
                .channel(ChatChannel.Telegram)
                .build();

        messageProcessorService.processIncomingMessage(company.getId(), message);
    }

    @Transactional
    public void processEmail(String companyEmailAddress, EmailResponse emailResponse) {
        log.info("Processing incoming email for account: {}", companyEmailAddress);
        Company company = configProvider.findCompanyByChannelIdentifier(companyEmailAddress, ChatChannel.Email)
                .orElseThrow(() -> {
                    log.error("Email configuration not found for address: {}", companyEmailAddress);
                    chatMetricsService.incrementChatOperationError(
                            "ProcessEmail.ConfigNotFound",
                            "unknown_company_email_" + companyEmailAddress,
                            ResourceNotFoundException.class.getSimpleName()
                    );
                    return new ResourceNotFoundException("Email configuration not found for address: " + companyEmailAddress);
                });

        String fullEmailContent = formatEmailContent(emailResponse);

        IncomingChannelMessage message = IncomingChannelMessage.builder()
                .channelSpecificUserId(emailResponse.getFrom())
                .messageContent(fullEmailContent)
                .channel(ChatChannel.Email)
                .build();

        messageProcessorService.processIncomingMessage(company.getId(), message);
    }

    @Transactional
    public void processVk(VkResponse vkResponse) {
        log.info("Processing incoming VK message from community: {}, user: {}", vkResponse.getCommunityId(), vkResponse.getFromId());
        Company company = configProvider.findCompanyByChannelIdentifier(String.valueOf(vkResponse.getCommunityId()), ChatChannel.VK)
                .orElseThrow(() -> {
                    log.error("VK configuration not found for community ID: {}", vkResponse.getCommunityId());
                    chatMetricsService.incrementChatOperationError(
                            "ProcessVk.ConfigNotFound",
                            "unknown_company_vk_comm_" + vkResponse.getCommunityId(),
                            ResourceNotFoundException.class.getSimpleName()
                    );
                    return new ResourceNotFoundException("VK configuration not found for community ID: " + vkResponse.getCommunityId());
                });

        IncomingChannelMessage message = IncomingChannelMessage.builder()
                .channelSpecificUserId(String.valueOf(vkResponse.getFromId()))
                .messageContent(vkResponse.getText())
                .channel(ChatChannel.VK)
                .externalChatId(String.valueOf(vkResponse.getPeerId()))
                .build();

        messageProcessorService.processIncomingMessage(company.getId(), message);
    }

    @Transactional
    public void processWhatsapp(WhatsappResponse whatsappResponse) {
        log.info("Processing incoming WhatsApp message from phone {} to phone ID {}",
                whatsappResponse.getFromPhoneNumber(), whatsappResponse.getRecipientPhoneNumberId());
        Company company = configProvider.findCompanyByChannelIdentifier(String.valueOf(whatsappResponse.getRecipientPhoneNumberId()), ChatChannel.WhatsApp)
                .orElseThrow(() -> {
                    log.error("WhatsApp configuration not found for phone number ID: {}", whatsappResponse.getRecipientPhoneNumberId());
                    chatMetricsService.incrementChatOperationError(
                            "ProcessWhatsapp.ConfigNotFound",
                            "unknown_company_wa_phoneid_" + whatsappResponse.getRecipientPhoneNumberId(),
                            ResourceNotFoundException.class.getSimpleName()
                    );
                    return new ResourceNotFoundException("WhatsApp configuration not found for phone number ID: " + whatsappResponse.getRecipientPhoneNumberId());
                });

        IncomingChannelMessage message = IncomingChannelMessage.builder()
                .channelSpecificUserId(whatsappResponse.getFromPhoneNumber())
                .messageContent(whatsappResponse.getText())
                .channel(ChatChannel.WhatsApp)
                .build();

        messageProcessorService.processIncomingMessage(company.getId(), message);
    }

    @Transactional
    public void processDialogXChat(DialogXChatIncomingMessage dialogXMessage) {
        log.info("Processing incoming DialogXChat message from widgetId: {}, sessionId: {}",
                dialogXMessage.getWidgetId(), dialogXMessage.getSessionId());
        Company company = configProvider.findCompanyByChannelIdentifier(dialogXMessage.getWidgetId(), ChatChannel.DialogX_Chat)
                .orElseThrow(() -> {
                    log.error("DialogXChat configuration not found for widgetId: {}", dialogXMessage.getWidgetId());

                    return new ResourceNotFoundException("DialogXChat configuration not found for widgetId: " + dialogXMessage.getWidgetId());
                });
        IncomingChannelMessage message = IncomingChannelMessage.builder()
                .channelSpecificUserId(dialogXMessage.getSessionId())
                .messageContent(dialogXMessage.getText())
                .channel(ChatChannel.DialogX_Chat)
                .externalChatId(dialogXMessage.getSessionId())
                .build();

        messageProcessorService.processIncomingMessage(company.getId(), message);
    }

    private String formatEmailContent(EmailResponse emailResponse) {
        return "Subject: " + emailResponse.getSubject() + "\n\n" + emailResponse.getContent();
    }
}

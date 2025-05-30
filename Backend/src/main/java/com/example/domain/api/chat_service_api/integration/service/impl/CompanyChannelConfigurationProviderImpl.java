package com.example.domain.api.chat_service_api.integration.service.impl;

import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.company_subscription_module.company.*;
import com.example.database.repository.company_subscription_module.*;
import com.example.domain.api.chat_service_api.integration.service.ICompanyChannelConfigurationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CompanyChannelConfigurationProviderImpl implements ICompanyChannelConfigurationProvider {

    private final CompanyTelegramConfigurationRepository telegramRepo;
    private final CompanyMailConfigurationRepository mailRepo;
    private final CompanyVkConfigurationRepository vkRepo;
    private final CompanyWhatsappConfigurationRepository whatsappRepo;
    private final CompanyDialogXChatConfigurationRepository dialogXChatRepo;

    @Override
    public Optional<Company> findCompanyByChannelIdentifier(String channelIdentifier, ChatChannel channel) {
        return switch (channel) {
            case Telegram -> telegramRepo.findByBotUsername(channelIdentifier).map(CompanyTelegramConfiguration::getCompany);
            case Email -> mailRepo.findByEmailAddress(channelIdentifier).map(CompanyMailConfiguration::getCompany);
            case VK -> vkRepo.findByCommunityId(Long.parseLong(channelIdentifier))
                    .map(CompanyVkConfiguration::getCompany);
            case WhatsApp -> whatsappRepo.findByPhoneNumberId(Long.valueOf(channelIdentifier)).map(CompanyWhatsappConfiguration::getCompany);
            case DialogX_Chat -> dialogXChatRepo.findByWidgetId(channelIdentifier).map(CompanyDialogXChatConfiguration::getCompany);
            default -> Optional.empty();
        };
    }
}

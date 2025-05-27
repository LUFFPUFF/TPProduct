package com.example.domain.api.chat_service_api.integration.service;

import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.company_subscription_module.company.Company;

import java.util.Optional;

public interface ICompanyChannelConfigurationProvider {

    Optional<Company> findCompanyByChannelIdentifier(String channelIdentifier, ChatChannel channel);
}

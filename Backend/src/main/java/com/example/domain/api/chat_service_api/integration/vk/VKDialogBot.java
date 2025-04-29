package com.example.domain.api.chat_service_api.integration.vk;

import com.example.domain.api.chat_service_api.integration.process_service.ClientCompanyProcessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class VKDialogBot {

    //TODO Пробелма в вытягивании API

    @Value("${vk.api.version}")
    private static String API_VERSION;

    private final BlockingQueue<VkResponse> messageQueue = new LinkedBlockingQueue<>();
    private final SimpMessagingTemplate messagingTemplate;
    private final ClientCompanyProcessService clientCompanyProcessService;
    private final String accessToken;
    private final String groupId;
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    public VKDialogBot(SimpMessagingTemplate messagingTemplate,
                       ClientCompanyProcessService clientCompanyProcessService,
                       String accessToken,
                       String groupId,
                       OkHttpClient okHttpClient,
                       ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.clientCompanyProcessService = clientCompanyProcessService;
        this.accessToken = accessToken;
        this.groupId = groupId;
        this.okHttpClient = okHttpClient;
        this.objectMapper = objectMapper;
    }


}

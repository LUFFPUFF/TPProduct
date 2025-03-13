package com.example.domain.api.chat_service_api.integration.mail.dialog_bot;

import com.example.database.model.company_subscription_module.company.CompanyMailConfiguration;
import com.example.database.repository.company_subscription_module.CompanyMailConfigurationRepository;
import com.example.domain.api.chat_service_api.integration.mail.parser.AbstractEmailParser;
import com.example.domain.api.chat_service_api.integration.mail.response.EmailResponse;
import com.example.domain.api.chat_service_api.integration.process_service.ClientCompanyProcessService;
import jakarta.annotation.PostConstruct;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

@Component
public class EmailDialogBot {
    private final CompanyMailConfigurationRepository mailConfigRepository;
    private final EmailParserFactory emailParserFactory;
    private final SimpMessagingTemplate messagingTemplate;
    private final ClientCompanyProcessService clientCompanyProcessService;
    private final BlockingQueue<EmailResponse> messageQueue = new LinkedBlockingQueue<>();
    private List<AbstractEmailParser> emailParsers;

    public EmailDialogBot(CompanyMailConfigurationRepository mailConfigRepository,
                          EmailParserFactory emailParserFactory,
                          SimpMessagingTemplate messagingTemplate,
                          ClientCompanyProcessService clientCompanyProcessService) {
        this.mailConfigRepository = mailConfigRepository;
        this.emailParserFactory = emailParserFactory;
        this.messagingTemplate = messagingTemplate;
        this.clientCompanyProcessService = clientCompanyProcessService;
    }

    @PostConstruct
    public void init() {
        List<CompanyMailConfiguration> emailConfigs = mailConfigRepository.findAll();

        emailParsers = emailConfigs.stream()
                .map(config -> emailParserFactory.createParser(config, messageQueue))
                .collect(Collectors.toList());

        startEmailListeners();
        startParsing();
    }

    private void startEmailListeners() {
        new Thread(() -> {
            while (true) {
                EmailResponse response = getResponse();

                clientCompanyProcessService.processEmail(response.getTo(), response);
                messagingTemplate.convertAndSend("/topic/email", response);
            }
        }).start();
    }

    public EmailResponse getResponse() {
        try {
            return messageQueue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Scheduled(fixedRate = 10000)
    public void startParsing() {
        emailParsers.forEach(AbstractEmailParser::startParsing);
    }
}

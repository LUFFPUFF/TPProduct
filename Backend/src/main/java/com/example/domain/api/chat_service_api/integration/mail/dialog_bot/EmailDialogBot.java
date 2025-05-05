package com.example.domain.api.chat_service_api.integration.mail.dialog_bot;

import com.example.database.model.company_subscription_module.company.CompanyMailConfiguration;
import com.example.database.repository.company_subscription_module.CompanyMailConfigurationRepository;
import com.example.domain.api.chat_service_api.integration.mail.parser.AbstractEmailParser;
import com.example.domain.api.chat_service_api.integration.mail.response.EmailResponse;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Component
public class EmailDialogBot {
    private final CompanyMailConfigurationRepository mailConfigRepository;
    private final EmailParserFactory emailParserFactory;
    private final BlockingQueue<Object> incomingMessageQueue;
    private final BlockingQueue<EmailResponse> internalParserQueue = new LinkedBlockingQueue<>();
    private final JavaMailSender mailSender;
    private List<AbstractEmailParser> emailParsers;

    public EmailDialogBot(CompanyMailConfigurationRepository mailConfigRepository,
                          EmailParserFactory emailParserFactory,
                          BlockingQueue<Object> incomingMessageQueue,
                          JavaMailSender mailSender) {
        this.mailConfigRepository = mailConfigRepository;
        this.emailParserFactory = emailParserFactory;
        this.incomingMessageQueue = incomingMessageQueue;
        this.mailSender = mailSender;
    }

    @PostConstruct
    public void init() {
        List<CompanyMailConfiguration> emailConfigs = mailConfigRepository.findAll();

        emailParsers = emailConfigs.stream()
                .map(config -> emailParserFactory.createParser(config, internalParserQueue))
                .toList();

        startInternalQueueProducer();
        startParsing();
    }

    public EmailResponse getResponse() {
        try {
            return internalParserQueue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Отправляет текстовое сообщение по Email.
     * @param toEmailAddress Адрес получателя.
     * @param subject Тема письма (часто нужно сохранить тему оригинального письма для "Reply").
     * @param content Содержимое письма.
     * @param fromEmailAddress Адрес отправителя (бота/компании).
     * @throws Exception В случае ошибки отправки (например, MessagingException).
     */
    public void sendMessage(String toEmailAddress,
                            String subject,
                            String content,
                            String fromEmailAddress) {
        if (toEmailAddress == null || toEmailAddress.trim().isEmpty() || fromEmailAddress == null
                || fromEmailAddress.trim().isEmpty() || content == null || content.trim().isEmpty()) {
            return;
        }

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);

            mimeMessageHelper.setFrom(fromEmailAddress);
            mimeMessageHelper.setTo(toEmailAddress);
            mimeMessageHelper.setSubject(subject);
            mimeMessageHelper.setText(content, false);

            mailSender.send(mimeMessage);

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }

    }

    private void startInternalQueueProducer() {
        new Thread(() -> {
            while (true) {
                try {
                    EmailResponse response = getResponse();
                    log.debug("Putting Email message from internal queue into main incoming queue: {}", response);
                    incomingMessageQueue.put(response);
                    log.info("Email message from {} added to incoming queue.", response.getFrom());

                } catch (InterruptedException e) {
                    log.error("Email message queue producer interrupted:", e);
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Error putting Email message into queue: {}", e.getMessage(), e);
                }
            }
        }).start();
        log.info("Email message queue producer started.");
    }

    @Scheduled(fixedRate = 10000)
    public void startParsing() {
        emailParsers.forEach(AbstractEmailParser::startParsing);
    }
}

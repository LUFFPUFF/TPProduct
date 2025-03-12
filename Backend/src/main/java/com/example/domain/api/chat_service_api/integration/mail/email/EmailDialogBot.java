package com.example.domain.api.chat_service_api.integration.mail.email;

import com.example.domain.api.chat_service_api.integration.process_service.ClientCompanyProcessService;
import jakarta.annotation.PostConstruct;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.mail.*;
import javax.mail.search.FlagTerm;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class EmailDialogBot {

    private final EmailProperties emailProperties;

    private final BlockingQueue<EmailResponse> messageQueue = new LinkedBlockingQueue<>();
    private final SimpMessagingTemplate messagingTemplate;
    private final ClientCompanyProcessService clientCompanyProcessService;

    public EmailDialogBot(EmailProperties emailProperties,
                          SimpMessagingTemplate messagingTemplate,
                          ClientCompanyProcessService clientCompanyProcessService) {
        this.emailProperties = emailProperties;
        this.messagingTemplate = messagingTemplate;
        this.clientCompanyProcessService = clientCompanyProcessService;
    }

    private Store connectToEmailServer() throws MessagingException {
        Properties properties = new Properties();
        Session emailSession = Session.getDefaultInstance(properties);

        Store store = emailSession.getStore(emailProperties.getProtocol());
        store.connect(emailProperties.getHost(), emailProperties.getUsername(), emailProperties.getPassword());
        return store;
    }

    @PostConstruct
    public void init() {
        startEmailListener();
        startParsing();
    }

    private void startEmailListener() {
        new Thread(() -> {
            while (true) {
                EmailResponse response = getResponse();
                clientCompanyProcessService.processGmail(emailProperties.getUsername(), response);
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
        try {
            Store store = connectToEmailServer();
            Folder folder = store.getFolder(emailProperties.getFolderName());
            folder.open(Folder.READ_WRITE);

            Message[] messages = folder
                    .search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

            for (Message message : messages) {
                process(message);

                message.setFlag(Flags.Flag.SEEN, true);
            }

            folder.close(false);
            store.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void process(Message message) {
        try {
            String subject = message.getSubject();
            String from = message.getFrom()[0].toString();
            String content = extractTextFromMessage(message);
            Date receivedDate = message.getSentDate();

            EmailResponse emailResponse = new EmailResponse(subject, from, content, receivedDate);
            messageQueue.add(emailResponse);


        } catch (MessagingException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String extractTextFromMessage(Message message) throws IOException, MessagingException {
        if (message.isMimeType("text/plain")) {
            return (String) message.getContent();
        } else if (message.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) message.getContent();
            return getTextFromMultipart(multipart);
        }
        return "[Unsupported Content Type]";
    }

    private String getTextFromMultipart(Multipart multipart) throws MessagingException, IOException {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            if (part.isMimeType("text/plain")) {
                return (String) part.getContent();
            }
        }
        return "[No Text Content Found]";
    }
}

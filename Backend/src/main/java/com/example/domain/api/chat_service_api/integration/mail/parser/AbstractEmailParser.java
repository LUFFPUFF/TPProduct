package com.example.domain.api.chat_service_api.integration.mail.parser;

import com.example.database.model.company_subscription_module.company.CompanyMailConfiguration;
import com.example.domain.api.chat_service_api.integration.mail.properties.EmailProperties;
import com.example.domain.api.chat_service_api.integration.mail.response.EmailResponse;
import org.springframework.scheduling.annotation.Scheduled;

import javax.mail.*;
import javax.mail.search.FlagTerm;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractEmailParser {

    protected final CompanyMailConfiguration emailConfig;
    private final EmailProperties emailProperties;
    protected final BlockingQueue<EmailResponse> messageQueue;

    public AbstractEmailParser(CompanyMailConfiguration emailConfig,
                               EmailProperties emailProperties,
                               BlockingQueue<EmailResponse> messageQueue) {
        this.emailConfig = emailConfig;
        this.emailProperties = emailProperties;
        this.messageQueue = messageQueue;
    }

    protected abstract Properties getMailProperties();

    private Store connectToEmailServer() throws MessagingException {
        Properties properties = getMailProperties();
        Session emailSession = Session.getDefaultInstance(properties);
        Store store = emailSession.getStore(emailProperties.getImapStore());
        store.connect(emailConfig.getImapServer(), emailConfig.getEmailAddress(), emailConfig.getAppPassword());
        return store;
    }

    @Scheduled(fixedRate = 10000)
    public void startParsing() {
        try {
            Store store = connectToEmailServer();
            Folder folder = store.getFolder(emailProperties.getImapFolder());
            folder.open(Folder.READ_WRITE);

            Message[] messages = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

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

            String to = extractEmail(extractRecipientsEmail(message)).toLowerCase();

            EmailResponse emailResponse = new EmailResponse(subject, from, to, content, receivedDate);
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

    private String extractRecipientsEmail(Message message) throws MessagingException {
        Address[] recipients = message.getRecipients(Message.RecipientType.TO);
        if (recipients != null && recipients.length > 0) {
            return recipients[0].toString();
        }
        return "[Unknown Recipient]";
    }

    private String extractEmail(String rawAddress) {
        Pattern pattern = Pattern.compile("[\\w.-]+@[\\w.-]+\\.\\w+");
        Matcher matcher = pattern.matcher(rawAddress);
        if (matcher.find()) {
            return matcher.group();
        }
        return "[Unknown Email]";
    }
}

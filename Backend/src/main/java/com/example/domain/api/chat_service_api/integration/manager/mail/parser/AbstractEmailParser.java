package com.example.domain.api.chat_service_api.integration.manager.mail.parser;

import com.example.database.model.company_subscription_module.company.CompanyMailConfiguration;
import com.example.domain.api.chat_service_api.integration.manager.mail.properties.EmailProperties;
import com.example.domain.api.chat_service_api.integration.manager.mail.response.EmailResponse;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.FlagTerm;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public abstract class AbstractEmailParser implements Runnable {

    protected final CompanyMailConfiguration emailConfig;
    protected final BlockingQueue<Object> incomingMessageQueue;
    protected final EmailProperties emailProperties;
    protected volatile boolean running = true;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w.-]+@[\\w.-]+\\.\\w+");
    private static final long DEFAULT_POLLING_INTERVAL_MS = 10000;
    private static final int CONNECTION_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 15000;

    public AbstractEmailParser(CompanyMailConfiguration emailConfig,
                               BlockingQueue<Object> incomingMessageQueue,
                               EmailProperties emailProperties) {
        this.emailConfig = emailConfig;
        this.incomingMessageQueue = incomingMessageQueue;
        this.emailProperties = emailProperties;
        log.debug("AbstractEmailParser created for account: {}", emailConfig.getEmailAddress()); // Лог в конструкторе
    }

    protected abstract Properties getMailProperties();

    private Store connectToEmailServer() throws MessagingException {
        Properties properties = getMailProperties();
        properties.put("mail.imap.connectiontimeout", String.valueOf(CONNECTION_TIMEOUT_MS));
        properties.put("mail.imap.timeout", String.valueOf(READ_TIMEOUT_MS));
        properties.put("mail.imap.fetchsize", "1048576");


        Session emailSession = Session.getInstance(properties);
        Store store = null;
        try {
            log.debug("Attempting to connect to IMAP server {} on port {} for account {}", emailConfig.getImapServer(), emailConfig.getImapPort(), emailConfig.getEmailAddress()); // Лог перед коннектом
            store = emailSession.getStore(emailProperties.getImapStore());
            store.connect(emailConfig.getImapServer(), emailConfig.getEmailAddress(), emailConfig.getAppPassword());
            log.debug("Successfully connected to IMAP server {} for account {}", emailConfig.getImapServer(), emailConfig.getEmailAddress());
            return store;
        } catch (AuthenticationFailedException e) {
            if (store != null && store.isConnected()) { try { store.close(); } catch (MessagingException ignored) {} }
            log.error("Authentication failed for account {}: {}", emailConfig.getEmailAddress(), e.getMessage());
            throw e;
        }
        catch (MessagingException e) {
            if (store != null && store.isConnected()) { try { store.close(); } catch (MessagingException ignored) {} }
            log.error("Failed to connect to IMAP server {} for account {}. Exception type: {}, Message: {}",
                    emailConfig.getImapServer(), emailConfig.getEmailAddress(), e.getClass().getName(), e.getMessage(), e);
            if (e.getNextException() != null) {
                log.error("Nested exception during connection for account {}: {}", emailConfig.getEmailAddress(), e.getNextException().getMessage(), e.getNextException());
            }
            throw e;
        }
    }

    @Override
    public void run() {
        log.info("Email polling thread started for account: {}", emailConfig.getEmailAddress());
        Store store = null;
        Folder folder = null;

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                log.debug("Polling cycle started for account: {}", emailConfig.getEmailAddress());
                store = connectToEmailServer();
                folder = store.getFolder(emailProperties.getImapFolder());

                if (!folder.exists()) {
                    log.error("IMAP folder '{}' not found for account {}", emailProperties.getImapFolder(), emailConfig.getEmailAddress());
                    running = false;
                    break;
                }

                folder.open(Folder.READ_WRITE);

                Message[] messages = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
                log.debug("Found {} unseen messages for account: {}", messages.length, emailConfig.getEmailAddress());

                for (Message message : messages) {
                    if (!running || Thread.currentThread().isInterrupted()) {
                        log.info("Polling interrupted during message processing loop for account: {}", emailConfig.getEmailAddress());
                        break;
                    }
                    try {
                        process(message);
                        message.setFlag(Flags.Flag.SEEN, true);
                        log.debug("Processed and marked as seen message from {}", getFromEmail(message));
                    } catch (Exception e) {
                        log.error("Error processing email message from {}: {}", getFromEmail(message), e.getMessage(), e);
                    }
                }


                if (folder != null && folder.isOpen()) { try { folder.close(false); } catch (MessagingException ignored) {} }
                if (store != null && store.isConnected()) { try { store.close(); } catch (MessagingException ignored) {} }
                store = null; folder = null;

                log.debug("Polling cycle finished successfully for account: {}", emailConfig.getEmailAddress());


            } catch (AuthenticationFailedException e) {
                log.error("Authentication failed during polling for account {}. Pausing for 1 minute.", emailConfig.getEmailAddress());
                try {
                    TimeUnit.MINUTES.sleep(1);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    running = false;
                }
            }
            catch (MessagingException e) {
                log.error("Messaging error during polling cycle for account {}. Retrying after pause. Message: {}", emailConfig.getEmailAddress(), e.getMessage());
                try {
                    TimeUnit.SECONDS.sleep(30);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    running = false;
                }
            } catch (Exception e) {
                log.error("Unexpected error during polling cycle for account {}. Retrying after pause. Message: {}", emailConfig.getEmailAddress(), e.getMessage(), e);
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    running = false;
                }
            } finally {

                if (folder != null && folder.isOpen()) {
                    try { folder.close(false); } catch (MessagingException ignored) {}
                }
                if (store != null && store.isConnected()) {
                    try { store.close(); } catch (MessagingException ignored) {}
                }
            }

            if (running && !Thread.currentThread().isInterrupted()) {
                long pollingInterval = emailProperties.getPollingIntervalMs() != null && emailProperties.getPollingIntervalMs() > 0 ? emailProperties.getPollingIntervalMs() : DEFAULT_POLLING_INTERVAL_MS;
                try {
                    log.debug("Sleeping for {}ms before next poll for account {}", pollingInterval, emailConfig.getEmailAddress());
                    TimeUnit.MILLISECONDS.sleep(pollingInterval);
                } catch (InterruptedException e) {
                    log.info("Sleep interrupted for account {}.", emailConfig.getEmailAddress());
                    Thread.currentThread().interrupt();
                    running = false;
                }
            }
        }
        log.info("Email polling thread stopped for account: {}", emailConfig.getEmailAddress());
    }

    public void stopPolling() {
        log.info("Stopping polling requested for account: {}", emailConfig.getEmailAddress());
        running = false;
    }

    private void process(Message message) throws MessagingException, IOException {
        String subject = message.getSubject();
        String from = getFromEmail(message);
        if (from == null) {
            log.warn("Could not extract sender email from message. Skipping processing. Message subject: {}", subject);
            return;
        }

        String content = extractTextFromMessage(message);
        Date receivedDate = message.getSentDate();

        String to = emailConfig.getEmailAddress().toLowerCase();

        EmailResponse emailResponse = new EmailResponse(subject, from, to, content, receivedDate);

        try {
            incomingMessageQueue.put(emailResponse);
            log.info("Put incoming email from {} (to {}) with subject '{}' into main queue.", from, to, subject != null ? subject : "");
        } catch (InterruptedException e) {
            log.warn("Interrupted while putting email response into incoming queue for account {}.", emailConfig.getEmailAddress(), e);
            Thread.currentThread().interrupt();
        }
    }

    private String getFromEmail(Message message) {
        try {
            Address[] fromAddresses = message.getFrom();
            if (fromAddresses != null && fromAddresses.length > 0) {
                return extractEmail(fromAddresses[0].toString());
            }
        } catch (MessagingException e) {
            log.warn("Could not get 'From' address from message for account {}: {}", emailConfig.getEmailAddress(), e.getMessage());
        }
        return null;
    }


    private String extractTextFromMessage(Message message) throws IOException, MessagingException {
        if (message.isMimeType("text/plain")) {
            return (String) message.getContent();
        } else if (message.isMimeType("text/html")) {
            return htmlToPlainText((String) message.getContent());
        } else if (message.isMimeType("multipart/*")) {
            return getTextFromMultipart((Multipart) message.getContent());
        }
        return "[Unsupported Content Type]";
    }

    private String htmlToPlainText(String html) {
        // TODO: Использовать более продвинутую библиотеку для парсинга HTML в текст
        return html != null ? html.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", "")
                .replaceAll(" ", " ")
                .replaceAll("<", "<")
                .replaceAll(">", ">")
                .replaceAll("&", "&")
                .replaceAll(" +", " ")
                .trim()
                : "";
    }

    private String getTextFromMultipart(Multipart multipart) throws MessagingException, IOException {
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            if (part.getContent() instanceof MimeMultipart) {
                content.append(getTextFromMultipart((MimeMultipart) part.getContent()));
            } else if (part.isMimeType("text/plain")) {
                content.append((String) part.getContent());
            } else if (part.isMimeType("text/html")) {
                content.append(htmlToPlainText((String) part.getContent()));
            }
        }
        return content.toString();
    }

    private String extractEmail(String rawAddress) {
        Matcher matcher = EMAIL_PATTERN.matcher(rawAddress);
        if (matcher.find()) {
            return matcher.group().toLowerCase();
        }
        return null;
    }

    public Integer getCompanyId() {
        return emailConfig.getCompany() != null ? emailConfig.getCompany().getId() : null;
    }

    public String getEmailAddress() {
        return emailConfig.getEmailAddress();
    }
}

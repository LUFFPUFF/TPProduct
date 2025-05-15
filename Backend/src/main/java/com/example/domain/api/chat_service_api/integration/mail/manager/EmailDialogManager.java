package com.example.domain.api.chat_service_api.integration.mail.manager;

import com.example.database.model.company_subscription_module.company.CompanyMailConfiguration;
import com.example.database.repository.company_subscription_module.CompanyMailConfigurationRepository;
import com.example.domain.api.chat_service_api.integration.mail.parser.AbstractEmailParser;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.*;

@Slf4j
@Component
public class EmailDialogManager {

    private final CompanyMailConfigurationRepository mailConfigRepository;
    private final EmailParserFactory emailParserFactory;
    private final Map<Integer, AbstractEmailParser> runningEmailParsers = new ConcurrentHashMap<>();
    private final Map<Integer, Future<?>> emailPollingTasks = new ConcurrentHashMap<>();
    private ExecutorService parserManagerExecutor;

    public EmailDialogManager(CompanyMailConfigurationRepository mailConfigRepository,
                              EmailParserFactory emailParserFactory) {
        this.mailConfigRepository = mailConfigRepository;
        this.emailParserFactory = emailParserFactory;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing EmailDialogBot...");
        List<CompanyMailConfiguration> emailConfigs = mailConfigRepository.findAll();
        log.info("Found {} email configurations in database.", emailConfigs.size());


        List<CompanyMailConfiguration> runnableConfigs = emailConfigs.stream()
                .filter(this::isConfigRunnable)
                .toList();
        log.info("Found {} runnable email configurations.", runnableConfigs.size());


        if (runnableConfigs.isEmpty()) {
            log.info("No runnable email configurations found. Email polling will not start on initialization.");

            parserManagerExecutor = Executors.newFixedThreadPool(1);
            log.info("EmailDialogBot initialization complete (no runnable configs).");
            return;
        }

        int poolSize = Math.max(1, runnableConfigs.size() + 2);
        log.info("Creating a thread pool with {} threads for email parser management.", poolSize);

        parserManagerExecutor = Executors.newFixedThreadPool(poolSize);

        log.info("Attempting to start polling processes for {} runnable configurations...", runnableConfigs.size());

        for (CompanyMailConfiguration config : runnableConfigs) {
            try {
                startPollingForCompanyInternal(config);
            } catch (Exception e) {
                log.error("Unexpected error during initial startPollingForCompanyInternal for email {} (company ID {}): {}",
                        config.getEmailAddress(), config.getCompany() != null ? config.getCompany().getId() : "N/A", e.getMessage(), e);
            }
        }
        log.info("EmailDialogBot initialization complete. Active polling processes: {}", runningEmailParsers.size());
    }

    @PreDestroy
    public void destroy() {
        runningEmailParsers.forEach((companyId, parser) -> {
            log.info("Stopping email polling for company ID {}", companyId);
            parser.stopPolling();
            Future<?> task = emailPollingTasks.remove(companyId);
            checkTask(companyId, task);
        });
        runningEmailParsers.clear();

        if (parserManagerExecutor != null) {
            parserManagerExecutor.shutdownNow();
            try {
                log.info("Waiting for email parser manager executor to terminate...");
                if (!parserManagerExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.warn("Email parser manager executor did not terminate gracefully within timeout.");
                } else {
                    log.info("Email parser manager executor terminated successfully.");
                }
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for email parser manager executor to shut down.", e);
                Thread.currentThread().interrupt();
            }
        }

        log.info("EmailDialogBot shutdown complete.");
    }

    /**
     * Отправляет текстовое сообщение по Email, используя конфигурацию отправителя из БД.
     * @param companyId ID компании, от имени которой отправляется письмо.
     * @param toEmailAddress Адрес получателя.
     * @param subject Тема письма.
     * @param content Содержимое письма.
     */
    public void sendMessage(Integer companyId,
                            String toEmailAddress,
                            String subject,
                            String content) {
        if (companyId == null) {
            log.error("Cannot send email: Missing company ID.");
            return;
        }

        if (toEmailAddress == null || toEmailAddress.trim().isEmpty() || content == null || content.trim().isEmpty()) {
            log.warn("Attempted to send email for company ID {} with missing information: to={}, content empty={}",
                    companyId, toEmailAddress, content != null && content.trim().isEmpty());
            return;
        }

        CompanyMailConfiguration mailConfig = mailConfigRepository.findByCompanyId(companyId)
                .orElseGet(() -> {
                    log.error("Email configuration not found for company ID {}. Cannot send email.", companyId);
                    return null;
                });

        if (mailConfig == null || mailConfig.getEmailAddress() == null || mailConfig.getEmailAddress().trim().isEmpty() ||
                mailConfig.getAppPassword() == null || mailConfig.getAppPassword().trim().isEmpty() ||
                mailConfig.getImapServer() == null || mailConfig.getImapServer().trim().isEmpty() ||
                mailConfig.getImapPort() == null) {
            log.error("Incomplete email configuration for company ID {}. Cannot send email. Email:{}, Pass empty:{}, IMAP Server:{}, IMAP Port:{}",
                    companyId, mailConfig != null ? mailConfig.getEmailAddress() : "N/A",
                    mailConfig != null && (mailConfig.getAppPassword() == null || mailConfig.getAppPassword().trim().isEmpty()),
                    mailConfig != null ? mailConfig.getImapServer() : "N/A",
                    mailConfig != null ? mailConfig.getImapPort() : "N/A");
            return;
        }

        String fromEmailAddress = mailConfig.getEmailAddress();
        String smtpHost = mailConfig.getSmtpServer() != null ? mailConfig.getSmtpServer() : mailConfig.getImapServer();
        int smtpPort = 587;

        JavaMailSenderImpl dynamicMailSender = new JavaMailSenderImpl();
        dynamicMailSender.setHost(smtpHost);
        dynamicMailSender.setPort(smtpPort);
        dynamicMailSender.setUsername(fromEmailAddress);
        dynamicMailSender.setPassword(mailConfig.getAppPassword());

        Properties props = dynamicMailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");

        //TODO пока что так, в проде такое нельзя делать
        props.put("mail.smtp.starttls.enable", "true");

        props.put("mail.smtp.ssl.trust", smtpHost);

        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        MimeMessage mimeMessage = dynamicMailSender.createMimeMessage();
        try {
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            mimeMessageHelper.setFrom(fromEmailAddress);
            mimeMessageHelper.setTo(toEmailAddress);
            mimeMessageHelper.setSubject(subject != null && !subject.trim().isEmpty() ? subject : "Без темы");
            mimeMessageHelper.setText(content, false);

            dynamicMailSender.send(mimeMessage);

        } catch (MailSendException e) {
            log.error("Failed to send email from {} to {} for company ID {}: {}. Cause: {}",
                    fromEmailAddress, toEmailAddress, companyId, e.getMessage(),
                    e.getRootCause() != null ? e.getRootCause().getMessage() : "N/A", e);
        }
        catch (MessagingException e) {
            log.error("Failed to send email from {} to {} for company ID {} due to messaging error: {}",
                    fromEmailAddress, toEmailAddress, companyId, e.getMessage(), e);
        }
        catch (Exception e) {
            log.error("Unexpected error sending email from {} to {} for company ID {}: {}",
                    fromEmailAddress, toEmailAddress, companyId, e.getMessage(), e);
        }

    }

    /**
     * Запускает или обновляет процесс поллинга для указанной компании (если у нее настроен Email).
     * Должен вызываться из сервисного слоя при сохранении/обновлении конфигурации почты пользователем.
     * @param companyId ID компании.
     */
    public void startOrUpdatePollingForCompany(Integer companyId) {
        log.info("Attempting to start or update email polling for company ID {}", companyId);
        mailConfigRepository.findByCompanyId(companyId)
                .ifPresentOrElse(
                        config -> {
                            if (isConfigRunnable(config)) {
                                stopPollingForCompany(companyId);
                                startPollingForCompanyInternal(config);
                                log.info("Email polling process started/updated for email {} (company ID {})", config.getEmailAddress(), companyId);
                            } else {
                                stopPollingForCompany(companyId);
                                log.warn("Email configuration for company ID {} is incomplete or inactive. Ensuring polling is stopped.", companyId);
                            }
                        },
                        () -> {
                            stopPollingForCompany(companyId);
                            log.warn("No Email configuration found for company ID {}. Ensuring polling is stopped.", companyId);
                        }
                );
    }

    /**
     * Останавливает процесс поллинга для указанной компании.
     * Должен вызываться из сервисного слоя при удалении конфигурации почты или при деактивации.
     * @param companyId ID компании.
     */
    public void stopPollingForCompany(Integer companyId) {
        AbstractEmailParser parser = runningEmailParsers.remove(companyId);
        Future<?> task = emailPollingTasks.remove(companyId);

        if (parser != null || task != null) {
            log.info("Stopping email polling process for company ID {}", companyId);
            if (parser != null) {
                parser.stopPolling();
            }
            checkTask(companyId, task);
            log.info("Email polling process stopped for company ID {}", companyId);
        } else {
            log.debug("No active email polling process found for company ID {} to stop.", companyId);
        }
    }

    private static void checkTask(Integer companyId, Future<?> task) {
        if (task != null && !task.isDone()) {
            log.info("Attempting to cancel email polling task for company ID {}", companyId);
            task.cancel(true);
            try {
                task.get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.warn("Error or timeout while waiting for task cancellation for company ID {}: {}", companyId, e.getMessage());
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            }
        }
    }


    private void startPollingForCompanyInternal(CompanyMailConfiguration config) {
        if (!isConfigRunnable(config)) {
            log.error("Cannot start email polling: Provided configuration is incomplete or invalid.");
            return;
        }

        Integer companyId = config.getCompany().getId();

        if (runningEmailParsers.containsKey(companyId)) {
            log.warn("Email polling process is already running for company ID {}. Skipping start for email {}", companyId, config.getEmailAddress());
            return;
        }

        log.info("Starting email polling process for email {} (company ID {})", config.getEmailAddress(), companyId);

        try {
            AbstractEmailParser parser = emailParserFactory.createParser(config);

            if (parserManagerExecutor == null || parserManagerExecutor.isShutdown()) {
                log.error("Email parser manager executor is not initialized or shut down. Cannot start polling for company ID {}.", companyId);
                return;
            }

            Future<?> task = parserManagerExecutor.submit(parser);

            runningEmailParsers.put(companyId, parser);
            emailPollingTasks.put(companyId, task);

            log.info("Email polling process submitted for email {} (company ID {}).", config.getEmailAddress(), companyId);

        } catch (IllegalArgumentException e) {
            log.error("Failed to create email parser for config (company ID {}): {}", companyId, e.getMessage());
        } catch (RejectedExecutionException e) {
            log.error("Failed to submit email polling task for company ID {} (ExecutorService is shutting down): {}", companyId, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error starting email parser for config (company ID {}): {}", companyId, e.getMessage(), e);
        }
    }

    private boolean isConfigRunnable(CompanyMailConfiguration config) {
        boolean runnable = config != null &&
                config.getCompany() != null &&
                config.getCompany().getId() != null &&
                config.getEmailAddress() != null && !config.getEmailAddress().trim().isEmpty() &&
                config.getAppPassword() != null && !config.getAppPassword().trim().isEmpty() &&
                config.getImapServer() != null && !config.getImapServer().trim().isEmpty();
        if (!runnable) {
            log.warn("Email config for company ID {} is not runnable. Details: config present={}, company present={}, company ID present={}, email empty={}, password empty={}, imap server empty={}",
                    config != null && config.getCompany() != null ? config.getCompany().getId() : "N/A",
                    config != null, config != null && config.getCompany() != null, config != null && config.getCompany() != null && config.getCompany().getId() != null,
                    config != null && (config.getEmailAddress() == null || config.getEmailAddress().trim().isEmpty()),
                    config != null && (config.getAppPassword() == null || config.getAppPassword().trim().isEmpty()),
                    config != null && (config.getImapServer() == null || config.getImapServer().trim().isEmpty())
            );
        }
        return runnable;
    }
}

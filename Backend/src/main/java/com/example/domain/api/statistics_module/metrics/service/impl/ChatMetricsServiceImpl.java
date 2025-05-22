package com.example.domain.api.statistics_module.metrics.service.impl;

import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.domain.api.statistics_module.metrics.service.IChatMetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMetricsServiceImpl implements IChatMetricsService {

    private final MeterRegistry registry;

    private static final String METRIC_PREFIX = "chat_app_";

    private static final String CHATS_CREATED_TOTAL = METRIC_PREFIX + "chats_total";
    private static final String CHATS_ASSIGNED_TOTAL = METRIC_PREFIX + "chats_assigned_total";
    private static final String CHATS_CLOSED_TOTAL = METRIC_PREFIX + "chats_closed_total";
    private static final String CHATS_ESCALATED_TOTAL = METRIC_PREFIX + "chats_escalated_total";
    private static final String CHATS_AR_HANDLED_TOTAL = METRIC_PREFIX + "chats_auto_responder_handled_total";
    private static final String CHATS_OPERATOR_LINKED_TOTAL = METRIC_PREFIX + "chats_operator_linked_total";

    private static final String CHAT_DURATION_SECONDS = METRIC_PREFIX + "chat_duration_seconds";
    private static final String CHAT_ASSIGNMENT_TIME_SECONDS = METRIC_PREFIX + "chat_assignment_time_seconds";
    private static final String CHAT_FIRST_RESPONSE_TIME_SECONDS = METRIC_PREFIX + "chat_first_operator_response_time_seconds";

    private static final String MESSAGES_SENT_TOTAL = METRIC_PREFIX + "messages_sent_total";
    private static final String MESSAGE_CONTENT_LENGTH_BYTES = METRIC_PREFIX + "message_content_length_bytes";
    private static final String MESSAGES_READ_BY_OPERATOR_TOTAL = METRIC_PREFIX + "messages_read_by_operator_total";
    private static final String MESSAGE_STATUS_UPDATED_TOTAL = METRIC_PREFIX + "message_status_updated_total";

    private static final String OPERATOR_MESSAGES_SENT_TOTAL = METRIC_PREFIX + "operator_messages_sent_total";
    private static final String CHAT_OPERATION_ERRORS_TOTAL = METRIC_PREFIX + "operation_errors_total";

    private static final String TAG_COMPANY_ID = "company_id";
    private static final String TAG_CHANNEL = "channel";
    private static final String TAG_FROM_OPERATOR_UI = "from_operator_ui";
    private static final String TAG_AUTO_ASSIGNED = "auto_assigned";
    private static final String TAG_FINAL_STATUS = "final_status";
    private static final String TAG_SENDER_TYPE = "sender_type";
    private static final String TAG_OPERATOR_ID = "operator_id";
    private static final String TAG_OPERATION_NAME = "operation";
    private static final String TAG_ERROR_TYPE = "error_type";
    private static final String TAG_NEW_STATUS = "new_status";

    @Override
    public void incrementChatsCreated(String companyId, ChatChannel channel, boolean fromOperatorUI) {
        String sanCompanyId = sanitizeTag(companyId);
        String sanChannel = sanitizeTag(channel != null ? channel.name() : "null_channel");
        String sanFromUI = String.valueOf(fromOperatorUI);

        log.info("Attempting to register/increment chats_created_total. companyId: '{}', channel: '{}', fromOperatorUI: '{}'",
                sanCompanyId, sanChannel, sanFromUI);
        try {
            Counter counter = Counter.builder(CHATS_CREATED_TOTAL)
                    .description("Общее количество созданных чатов")
                    .tag(TAG_COMPANY_ID, sanCompanyId)
                    .tag(TAG_CHANNEL, sanChannel)
                    .tag(TAG_FROM_OPERATOR_UI, sanFromUI)
                    .register(registry);
            counter.increment();
            log.info("Successfully incremented chats_created_total for companyId: {}, channel: {}", sanCompanyId, sanChannel);
        } catch (Exception e) {
            log.error("ERROR registering/incrementing chats_created_total: companyId={}, channel={}, fromUI={}",
                    sanCompanyId, sanChannel, sanFromUI, e);
        }
    }

    @Override
    public void incrementChatsAssigned(String companyId, ChatChannel channel, boolean autoAssigned) {
        Counter.builder(CHATS_ASSIGNED_TOTAL)
                .description("Общее количество чатов, назначенных оператору")
                .tag(TAG_COMPANY_ID, sanitizeTag(companyId))
                .tag(TAG_CHANNEL, sanitizeTag(channel.name()))
                .tag(TAG_AUTO_ASSIGNED, String.valueOf(autoAssigned))
                .register(registry)
                .increment();
    }

    @Override
    public void incrementChatsClosed(String companyId, ChatChannel channel, ChatStatus finalStatus) {
        Counter.builder(CHATS_CLOSED_TOTAL)
                .description("Общее количество закрытых чатов")
                .tag(TAG_COMPANY_ID, sanitizeTag(companyId))
                .tag(TAG_CHANNEL, sanitizeTag(channel.name()))
                .tag(TAG_FINAL_STATUS, sanitizeTag(finalStatus.name()))
                .register(registry)
                .increment();
    }

    @Override
    public void incrementChatsEscalated(String companyId, ChatChannel channel) {
        Counter.builder(CHATS_ESCALATED_TOTAL)
                .description("Общее количество чатов, переданных оператору")
                .tag(TAG_COMPANY_ID, sanitizeTag(companyId))
                .tag(TAG_CHANNEL, sanitizeTag(channel.name()))
                .register(registry)
                .increment();
    }

    @Override
    public void incrementChatsAutoResponderHandled(String companyId, ChatChannel channel) {
        Counter.builder(CHATS_AR_HANDLED_TOTAL)
                .description("Общее количество чатов, изначально обработанных автоответчиком")
                .tag(TAG_COMPANY_ID, sanitizeTag(companyId))
                .tag(TAG_CHANNEL, sanitizeTag(channel.name()))
                .register(registry)
                .increment();
    }

    @Override
    public void incrementChatOperatorLinked(String companyId, ChatChannel channel) {
        Counter.builder(CHATS_OPERATOR_LINKED_TOTAL)
                .description("Общее количество раз, когда оператор подключался к чату (не обязательно при первом назначении)")
                .tag(TAG_COMPANY_ID, sanitizeTag(companyId))
                .tag(TAG_CHANNEL, sanitizeTag(channel.name()))
                .register(registry)
                .increment();
    }

    @Override
    public void recordChatDuration(String companyId, ChatChannel channel, ChatStatus finalStatus, Duration duration) {
        Timer.builder(CHAT_DURATION_SECONDS)
                .description("Продолжительность чатов с момента создания до закрытия")
                .tag(TAG_COMPANY_ID, sanitizeTag(companyId))
                .tag(TAG_CHANNEL, sanitizeTag(channel.name()))
                .tag(TAG_FINAL_STATUS, sanitizeTag(finalStatus.name()))
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .sla(Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(15), Duration.ofMinutes(30))
                .register(registry)
                .record(duration);
    }

    @Override
    public void recordChatAssignmentTime(String companyId, ChatChannel channel, Duration duration) {
        Timer.builder(CHAT_ASSIGNMENT_TIME_SECONDS)
                .description("Время, необходимое для назначения чата оператору")
                .tag(TAG_COMPANY_ID, sanitizeTag(companyId))
                .tag(TAG_CHANNEL, sanitizeTag(channel.name()))
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .sla(Duration.ofSeconds(10), Duration.ofSeconds(30), Duration.ofMinutes(1), Duration.ofMinutes(5))
                .register(registry)
                .record(duration);
    }

    @Override
    public void recordChatFirstOperatorResponseTime(String companyId, ChatChannel channel, Duration duration) {
        Timer.builder(CHAT_FIRST_RESPONSE_TIME_SECONDS)
                .description("Время, необходимое оператору для первого ответа после первоначального сообщения или задания клиента")
                .tag(TAG_COMPANY_ID, sanitizeTag(companyId))
                .tag(TAG_CHANNEL, sanitizeTag(channel.name()))
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .sla(Duration.ofSeconds(15), Duration.ofSeconds(45), Duration.ofMinutes(2), Duration.ofMinutes(10))
                .register(registry)
                .record(duration);
    }

    @Override
    public void incrementMessagesSent(String companyId, ChatChannel channel, ChatMessageSenderType senderType) {
        Counter.builder(MESSAGES_SENT_TOTAL)
                .description("Общее количество отправленных сообщений")
                .tag(TAG_COMPANY_ID, sanitizeTag(companyId))
                .tag(TAG_CHANNEL, sanitizeTag(channel.name()))
                .tag(TAG_SENDER_TYPE, sanitizeTag(senderType.name()))
                .register(registry)
                .increment();
    }

    @Override
    public void recordMessageContentLength(String companyId, ChatChannel channel, ChatMessageSenderType senderType, int length) {
        registry.summary(MESSAGE_CONTENT_LENGTH_BYTES,
                        TAG_COMPANY_ID, sanitizeTag(companyId),
                        TAG_CHANNEL, sanitizeTag(channel.name()),
                        TAG_SENDER_TYPE, sanitizeTag(senderType.name()))
                .record(length);
    }

    @Override
    public void incrementMessagesReadByOperator(String companyId, ChatChannel channel) {
        Counter.builder(MESSAGES_READ_BY_OPERATOR_TOTAL)
                .description("Общее количество клиентских сообщений, помеченных оператором как прочитанные")
                .tag(TAG_COMPANY_ID, sanitizeTag(companyId))
                .tag(TAG_CHANNEL, sanitizeTag(channel.name()))
                .register(registry)
                .increment();
    }

    @Override
    public void incrementMessageStatusUpdated(String companyId, ChatChannel channel, String newStatus) {
        Counter.builder(MESSAGE_STATUS_UPDATED_TOTAL)
                .description("Общее количество обновлений статуса сообщения")
                .tag(TAG_COMPANY_ID, sanitizeTag(companyId))
                .tag(TAG_CHANNEL, sanitizeTag(channel.name()))
                .tag(TAG_NEW_STATUS, sanitizeTag(newStatus))
                .register(registry)
                .increment();
    }

    @Override
    public void incrementOperatorMessagesSent(String companyId, String operatorId, ChatChannel channel) {
        Counter.builder(OPERATOR_MESSAGES_SENT_TOTAL)
                .description("Общее количество сообщений, отправленных конкретным оператором")
                .tag(TAG_COMPANY_ID, sanitizeTag(companyId))
                .tag(TAG_OPERATOR_ID, sanitizeTag(operatorId))
                .tag(TAG_CHANNEL, sanitizeTag(channel.name()))
                .register(registry)
                .increment();
    }

    @Override
    public void incrementChatOperationError(String operationName, String companyId, String errorType) {
        log.warn("Chat operation error: {}, company: {}, type: {}", operationName, companyId, errorType);
        Counter.builder(CHAT_OPERATION_ERRORS_TOTAL)
                .description("Общее количество ошибок, обнаруженных во время работы чата")
                .tag(TAG_OPERATION_NAME, sanitizeTag(operationName))
                .tag(TAG_COMPANY_ID, sanitizeTag(companyId))
                .tag(TAG_ERROR_TYPE, sanitizeTag(errorType))
                .register(registry)
                .increment();
    }

    private String sanitizeTag(String value) {
        if (value == null || value.isEmpty()) {
            return "unknown";
        }
        return value.replaceAll("[^a-zA-Z0-9_]", "_");
    }
}

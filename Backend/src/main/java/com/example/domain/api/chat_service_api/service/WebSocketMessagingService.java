package com.example.domain.api.chat_service_api.service;

import com.example.domain.api.chat_service_api.config.WebSocketTopicRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketMessagingService {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketTopicRegistry topicRegistry;

    /**
     * Отправляет сообщение в публичный STOMP топик (модель publish-subscribe).
     * Сообщение будет доставлено всем клиентам, подписанным на указанный {@code destination}.
     *
     * @param destination Полный путь к STOMP топику (например, "/topic/some-channel").
     * @param payload     Объект, который будет сериализован (обычно в JSON) и отправлен как тело сообщения.
     */
    public void sendMessage(String destination, Object payload) {
        log.debug("Sending STOMP message to destination '{}'. Payload: {}", destination, payload);
        messagingTemplate.convertAndSend(destination, payload);
    }

    /**
     * Отправляет сообщение конкретному пользователю (модель point-to-point).
     * Использует механизм пользовательских очередей STOMP, обычно с префиксом "/user".
     * {@link SimpMessagingTemplate#convertAndSendToUser(String, String, Object)} автоматически
     * разрешает сессию пользователя и формирует конечный адрес.
     *
     * @param userId            Идентификатор пользователя (обычно это {@code principal.getName()} аутентифицированного пользователя).
     * @param destinationSuffix Суффикс пути, который будет добавлен к префиксу пользовательской очереди
     *                          (например, "/notifications" или "/queue/private-messages").
     *                          Конечный путь будет сформирован примерно как "/user/{userId}{destinationSuffix}".
     * @param payload           Объект для отправки.
     */
    public void sendToUser(String userId, String destinationSuffix, Object payload) {
        log.debug("Sending STOMP message to user '{}', user-specific destination suffix '{}'. Payload: {}",
                userId, destinationSuffix, payload);
        messagingTemplate.convertAndSendToUser(userId, destinationSuffix, payload);
    }

    /**
     * Отправляет новое сообщение в указанный чат.
     * Использует топик, определенный в {@link WebSocketTopicRegistry#getChatMessagesTopic(Object)}.
     *
     * @param chatId         ID чата, в который отправляется сообщение.
     * @param messagePayload DTO сообщения (например, {@code MessageDto}).
     */
    public void sendChatMessage(Integer chatId, Object messagePayload) {
        log.info("Sending new chat message to chat ID {}.", chatId);
        sendMessage(topicRegistry.getChatMessagesTopic(chatId), messagePayload);
    }

    /**
     * Отправляет обновление статуса для одного или нескольких сообщений в указанном чате.
     * Например, для отметки сообщений как "доставлено" или "прочитано".
     * По умолчанию отправляется на тот же топик, что и новые сообщения.
     * Клиент должен уметь идентифицировать и обновить существующее сообщение по данным из {@code messageStatusPayload}.
     *
     * @param chatId               ID чата, к которому относится обновление.
     * @param messageStatusPayload DTO с информацией об обновлении статуса (например, обновленный {@code MessageDto}
     *                             или специальный DTO для обновления статуса).
     */
    public void sendChatMessageStatusUpdate(Integer chatId, Object messageStatusPayload) {
        log.info("Sending chat message status update to chat ID {}.", chatId);
        sendMessage(topicRegistry.getChatMessagesTopic(chatId), messageStatusPayload);
    }

    /**
     * Отправляет уведомление в указанный чат.
     *
     * @param chatId             ID чата.
     * @param notificationPayload DTO уведомления.
     */
    public void sendChatNotification(Integer chatId, Object notificationPayload) {
        log.info("Sending notification to chat ID {}.", chatId);
        sendMessage(topicRegistry.getChatNotificationsTopic(chatId), notificationPayload);
    }

    /**
     * Отправляет обновление общего статуса чата.
     * Например, изменение статуса чата (PENDING, ASSIGNED, CLOSED), обновление времени последнего сообщения,
     * или назначение/изменение оператора.
     * Использует топик, определенный в {@link WebSocketTopicRegistry#getChatStatusTopic(Object)}.
     *
     * @param chatId        ID чата, статус которого обновляется.
     * @param statusPayload DTO с обновленным состоянием чата (например, {@code ChatDTO}).
     */
    public void sendChatStatusUpdate(Integer chatId, Object statusPayload) {
        log.info("Sending chat status update for chat ID {}.", chatId);
        sendMessage(topicRegistry.getChatStatusTopic(chatId), statusPayload);
    }

    /**
     * Отправляет событие "печатает" (typing indicator) для указанного чата.
     * Используется для уведомления участников чата о том, что кто-то набирает сообщение.
     * Использует топик, определенный в {@link WebSocketTopicRegistry#getChatTypingTopic(Object)}.
     *
     * @param chatId        ID чата.
     * @param typingPayload DTO, содержащее информацию о событии печати (например, кто печатает и флаг isTyping).
     */
    public void sendChatTypingUpdate(Integer chatId, Object typingPayload) {
        log.debug("Sending chat typing update for chat ID {}.", chatId);
        sendMessage(topicRegistry.getChatTypingTopic(chatId), typingPayload);
    }

    /**
     * Отправляет общее уведомление конкретному пользователю.
     * Использует суффикс топика из {@link WebSocketTopicRegistry#getUserNotificationsQueueSuffix()}.
     *
     * @param userId              ID пользователя (principal name).
     * @param notificationPayload DTO уведомления.
     */
    public void notifyUserGeneral(String userId, Object notificationPayload) {
        log.info("Notifying user '{}' with a general notification.", userId);
        sendToUser(userId, topicRegistry.getUserNotificationsQueueSuffix(), notificationPayload);
    }

    /**
     * Уведомляет оператора о том, что ему был назначен новый чат.
     * Использует суффикс топика из {@link WebSocketTopicRegistry#getUserAssignedChatsQueueSuffix()}.
     *
     * @param operatorUserId ID оператора (principal name).
     * @param chatPayload    DTO назначенного чата (например, {@code ChatDTO}).
     */
    public void notifyOperatorAboutAssignedChat(String operatorUserId, Object chatPayload) {
        log.info("Notifying operator '{}' about newly assigned chat.", operatorUserId);
        sendToUser(operatorUserId, topicRegistry.getUserAssignedChatsQueueSuffix(), chatPayload);
    }

    /**
     * Уведомляет оператора о том, что один из его чатов был закрыт.
     * Использует суффикс топика из {@link WebSocketTopicRegistry#getUserChatClosedQueueSuffix()}.
     *
     * @param operatorUserId ID оператора (principal name).
     * @param chatPayload    DTO закрытого чата (например, {@code ChatDTO}).
     */
    public void notifyOperatorAboutClosedChat(String operatorUserId, Object chatPayload) {
        log.info("Notifying operator '{}' about a closed chat.", operatorUserId);
        sendToUser(operatorUserId, topicRegistry.getUserChatClosedQueueSuffix(), chatPayload);
    }

    /**
     * Рассылает информацию о новом чате, ожидающем оператора, всем подписчикам топика компании.
     * Используется для обновления очередей ожидания у менеджеров или доступных операторов.
     * Использует топик из {@link WebSocketTopicRegistry#getCompanyPendingChatsTopic(Object)}.
     *
     * @param companyId   ID компании.
     * @param chatPayload DTO нового ожидающего чата (например, {@code ChatDTO}).
     */
    public void broadcastNewPendingChatToCompany(Integer companyId, Object chatPayload) {
        log.info("Broadcasting new pending chat for company ID {}.", companyId);
        sendMessage(topicRegistry.getCompanyPendingChatsTopic(companyId), chatPayload);
    }

    /**
     * Рассылает информацию о назначенном чате всем подписчикам соответствующего топика компании.
     * Может использоваться для общей ленты назначенных чатов, доступной, например, менеджерам.
     * Использует топик из {@link WebSocketTopicRegistry#getCompanyAssignedChatsTopic(Object)}.
     *
     * @param companyId   ID компании.
     * @param chatPayload DTO назначенного чата (например, {@code ChatDTO}).
     */
    public void broadcastAssignedChatToCompany(Integer companyId, Object chatPayload) {
        log.info("Broadcasting assigned chat for company ID {}.", companyId);
        sendMessage(topicRegistry.getCompanyAssignedChatsTopic(companyId), chatPayload);
    }
}

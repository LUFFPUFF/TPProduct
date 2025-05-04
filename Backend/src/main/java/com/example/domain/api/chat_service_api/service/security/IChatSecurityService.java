package com.example.domain.api.chat_service_api.service.security;

import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import org.springframework.security.core.Authentication;

import java.util.Optional;

public interface IChatSecurityService {

    /**
     * Проверяет, является ли текущий аутентифицированный principal AppUserDetails (пользователем сервиса)
     * и имеет ли он роль Оператора или Менеджера с привязкой к компании.
     */
    boolean isAppUserOperatorOrManagerWithCompany(Authentication authentication);

    /**
     * Проверяет, принадлежит ли чат компании текущего аутентифицированного оператора/менеджера.
     * Этот метод используется для ЧТЕНИЯ или других действий, доступных только User'ам.
     */
    boolean canAccessChat(Integer chatId);

    /**
     * Проверяет, может ли текущий аутентифицированный оператор/менеджер получить доступ к клиенту.
     * Разрешено, если пользователь является оператором/менеджером с компанией, и клиент привязан к той же компании.
     */
    boolean canAccessClient(Integer clientId);

    /**
     * Проверяет, может ли текущий аутентифицированный оператор/менеджер назначить оператора на чат.
     * Разрешено, если пользователь является оператором/менеджером с компанией,
     * и чат принадлежит той же компании.
     */
    boolean canAssignOperatorToChat(Integer chatId);

    /**
     * Проверяет, может ли текущий аутентифицированный оператор/менеджер закрыть чат.
     * Разрешено, если пользователь является оператором/менеджером с компанией,
     * и чат принадлежит той же компании.
     * Дополнительно:
     * - Если это OPERATOR, он должен быть назначен на этот чат.
     * - Если это MANAGER, ему достаточно принадлежать к компании чата.
     */
    boolean canCloseChat(Integer chatId);

    /**
     * Проверяет, может ли текущий аутентифицированный оператор/менеджер обновить статус чата.
     * Аналогично canCloseChat, но более общее.
     */
    boolean canUpdateChatStatus(Integer chatId);

    /**
     * Определяет, может ли текущий аутентифицированный субъект (principal)
     * отправить сообщение в указанный чат с указанным типом отправителя и ID.
     * - Если principal является User (Operator/Manager): должен отправлять ОТ СЕБЯ как OPERATOR в чат СВОЕЙ компании.
     * - Если principal НЕ является User (например, Client via external channel): может отправлять как CLIENT/AUTO_RESPONDER.
     */
    boolean canProcessAndSaveMessage(Integer chatId, Integer senderId, ChatMessageSenderType senderType);

    /**
     * Проверяет, может ли текущий аутентифицированный оператор/менеджер обновить статус сообщения.
     * Разрешено, если пользователь является оператором/менеджером с компанией, и сообщение находится в чате этой компании.
     */
    boolean canUpdateMessageStatus(Integer messageId);

    /**
     * Проверяет, может ли текущий аутентифицированный оператор/менеджер пометить сообщения как прочитанные в указанном чате.
     * Должен быть оператором/менеджером, иметь доступ к чату по компании.
     * Дополнительная проверка:
     * - Если это OPERATOR, он должен быть назначен на этот чат.
     * - Если это MANAGER, ему достаточно принадлежать к компании чата (менеджер может видеть и отмечать сообщения во всех чатах своей компании).
     * RequestingOperatorId должен совпадать с ID аутентифицированного пользователя (это проверено в @PreAuthorize).
     */
    boolean canMarkMessagesAsRead(Integer chatId, Integer requestedOperatorId);

    /**
     * Проверяет, может ли текущий аутентифицированный оператор/менеджер обновить статус сообщения по externalId.
     * Разрешено, если пользователь является оператором/менеджером с компанией, и сообщение находится в чате этой компании.
     */
    boolean canUpdateMessageStatusByExternalId(Integer chatId, String externalMessageId);

}

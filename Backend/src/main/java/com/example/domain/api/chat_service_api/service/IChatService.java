package com.example.domain.api.chat_service_api.service;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.domain.api.chat_service_api.model.dto.ChatDTO;
import com.example.domain.api.chat_service_api.model.dto.ChatDetailsDTO;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.api.chat_service_api.model.rest.chat.AssignChatRequestDTO;
import com.example.domain.api.chat_service_api.model.rest.chat.CreateChatRequestDTO;

import java.nio.file.AccessDeniedException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface IChatService {

    /**
     * Создает новый чат.
     * @param createRequest DTO с данными для создания чата.
     * @return Созданный ChatDetailsDTO.
     */
    ChatDetailsDTO createChat(CreateChatRequestDTO createRequest);

    /**
     * Создает новый чат с привязкой к текущему авторизованному оператору/менеджеру.
     * Используется из UI. Проверяет принадлежность клиента к компании пользователя.
     * @param createRequest DTO с данными для создания чата (client ID, channel, initial message).
     * @return Созданный ChatDetailsDTO или существующий открытый чат.
     * @throws AccessDeniedException если клиент из другой компании.
     */
    ChatDetailsDTO createChatWithOperatorFromUI(CreateChatRequestDTO createRequest) throws AccessDeniedException;

    /**
     * Запрашивает эскалацию чата до оператора.
     * Может использоваться клиентом или, возможно, менеджером.
     * Если вызывается из UI менеджером, должна быть проверка доступа к чату.
     * @param chatId ID чата.
     * @param clientId ID клиента чата (для верификации).
     * @return Обновленный ChatDetailsDTO.
     */
    ChatDetailsDTO requestOperatorEscalation(Integer chatId, Integer clientId);

    /**
     * Привязывает указанного оператора к чату. Доступно только менеджерам в рамках своей компании.
     * @param chatId ID чата.
     * @param operatorId ID оператора для привязки.
     */
    void linkOperatorToChat(Integer chatId, Integer operatorId);

    /**
     * Находит первый открытый чат для клиента на указанном канале.
     * Предполагается для внутренней логики или обработки входящих сообщений, не привязан к UserContext.
     * @param clientId ID клиента.
     * @param channel Канал чата.
     * @return Optional с найденным чатом.
     */
    Optional<Chat> findOpenChatByClientAndChannel(Integer clientId, ChatChannel channel);

    /**
     * Находит первый открытый чат для клиента (без учета канала).
     * Предполагается для внутренней логики, не привязан к UserContext.
     * @param clientId ID клиента.
     * @return Optional с найденным чатом.
     */
    Optional<Chat> findOpenChatByClient(Integer clientId);

    /**
     * Находит сущность чата по ID. Требует прав доступа к чату согласно роли пользователя.
     * @param chatId ID чата.
     * @return Optional с найденным чатом.
     */
    Optional<Chat> findChatEntityById(Integer chatId);

    /**
     * Назначает оператора на чат. Доступно только менеджерам.
     * @param assignRequest DTO с ID чата и, опционально, ID назначаемого оператора.
     * @return Обновленный ChatDetailsDTO.
     * @throws AccessDeniedException если у пользователя нет прав или чат/оператор из другой компании.
     */
    ChatDetailsDTO assignChat(AssignChatRequestDTO assignRequest) throws AccessDeniedException;

    /**
     * Закрывает чат текущим авторизованным пользователем (оператором/менеджером).
     * @param chatId ID чата для закрытия.
     * @return Обновленный ChatDetailsDTO.
     */
    ChatDetailsDTO closeChatByCurrentUser(Integer chatId);

    /**
     * Получает полные детали конкретного чата по его ID. Требует прав доступа к чату согласно роли пользователя.
     * @param chatId ID чата.
     * @return ChatDetailsDTO.
     */
    ChatDetailsDTO getChatDetails(Integer chatId);


    List<ChatDTO> getOperatorChats(Integer userId);

    List<ChatDTO> getOperatorChatsStatus(Integer userId, Set<ChatStatus> statuses);

    List<ChatDTO> getMyChats(Set<ChatStatus> statuses) throws AccessDeniedException;

    List<ChatDTO> getChatsForCurrentUser(Set<ChatStatus> statuses);

    /**
     * Получает список чатов, назначенных указанному оператору. Доступно только менеджерам в рамках своей компании.
     * @param operatorId ID оператора, чьи чаты нужно получить.
     * @param statuses Набор статусов для фильтрации. Если null или пустой, используются дефолтные статусы (ASSIGNED, IN_PROGRESS).
     * @return Список ChatDTO.
     * @throws AccessDeniedException если у пользователя нет прав или оператор из другой компании.
     */
    List<ChatDTO> getOperatorChats(Integer operatorId, Set<ChatStatus> statuses) throws AccessDeniedException;

    /**
     * Получает список чатов для указанного клиента. Доступно операторам и менеджерам в рамках своей компании.
     * @param clientId ID клиента.
     * @return Список ChatDTO.
     * @throws AccessDeniedException если у пользователя нет прав или клиент из другой компании.
     */
    List<ChatDTO> getClientChats(Integer clientId);

    /**
     * Получает список ожидающих чатов (статус PENDING_OPERATOR) для компании текущего авторизованного пользователя.
     * Доступно операторам и менеджерам.
     * @return Список ChatDTO.
     * @throws AccessDeniedException если у пользователя нет прав или он не привязан к компании.
     */
    List<ChatDTO> getMyCompanyWaitingChats() throws AccessDeniedException;

    /**
     * Обновляет статус чата. Доступно операторам и менеджерам в рамках своей компании.
     * @param chatId ID чата.
     * @param newStatus Новый статус чата.
     */
    void updateChatStatus(Integer chatId, ChatStatus newStatus);

    /**
     * Находит чат по внешнему ID, ID компании и каналу.
     * Используется для обработки входящих сообщений из внешних систем, не привязан к UserContext.
     * @param companyId ID компании.
     * @param clientId ID клиента (опционально, может быть не во всех внешних системах).
     * @param channel Канал чата.
     * @param externalChatId Внешний ID чата из сторонней системы.
     * @return Optional с найденным чатом.
     */
    Optional<Chat> findChatByExternalId(Integer companyId, Integer clientId, ChatChannel channel, String externalChatId);

    /**
     * Отправляет сообщение в чат от лица текущего авторизованного оператора/менеджера.
     * Проверяет права доступа к чату.
     * @param chatId ID чата, куда отправляется сообщение.
     * @param content Текст сообщения.
     * @return DTO отправленного сообщения.
     */
    MessageDto sendOperatorMessage(Integer chatId, String content);

    /**
     * Создает тестовый чат для текущего авторизованного оператора/менеджера.
     * @return Детали созданного тестового чата.
     * @throws AccessDeniedException если у пользователя нет прав или он не привязан к компании.
     */
    ChatDetailsDTO createTestChatForCurrentUser() throws AccessDeniedException;

    /**
     * Помечает сообщения в чате как прочитанные текущим авторизованным оператором/менеджером.
     * Проверяет права доступа к чату.
     * @param chatId ID чата, в котором помечаются сообщения.
     * @param messageIds Список ID сообщений для пометки.
     */
    void markClientMessagesAsReadByCurrentUser(Integer chatId, Collection<Integer> messageIds);

}

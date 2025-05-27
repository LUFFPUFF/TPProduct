package com.example.domain.api.chat_service_api.service.chat;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.domain.api.chat_service_api.exception_handler.ChatNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.exception.service.ChatServiceException;
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
     * Создает новый чат, обычно инициированный клиентом или внешней системой.
     * Чат может быть первоначально обработан автоответчиком.
     * Этот метод не требует контекста аутентифицированного пользователя для самого создания,
     * так как часто вызывается неаутентифицированными действиями клиента.
     *
     * @param createRequest DTO, содержащий данные для создания чата (ID клиента, канал, начальное сообщение, внешний ID).
     * @return {@link ChatDetailsDTO} созданного чата или существующего открытого чата, если найден.
     * @throws ResourceNotFoundException если клиент, указанный в запросе, не найден или не связан с компанией.
     * @throws ChatServiceException      для других специфичных ошибок сервиса во время создания чата.
     * @throws com.example.domain.api.ans_api_module.exception.AutoResponderException если возникает ошибка во время обработки автоответчиком.
     */
    ChatDetailsDTO createChat(CreateChatRequestDTO createRequest);

    /**
     * Создает новый чат, инициированный аутентифицированным оператором или менеджером через пользовательский интерфейс.
     * Чат немедленно назначается создающему оператору.
     * Проверяет, принадлежит ли клиент той же компании, что и аутентифицированный пользователь.
     *
     * @param createRequest DTO, содержащий данные для создания чата (ID клиента, канал, начальное сообщение).
     * @return {@link ChatDetailsDTO} созданного чата или существующего открытого чата, если найден.
     * @throws AccessDeniedException     если клиент принадлежит другой компании, чем пользователь,
     *                                   или если у пользователя нет разрешения на создание чатов.
     * @throws ResourceNotFoundException если клиент или аутентифицированный пользователь не найден.
     * @throws ChatServiceException      для других специфичных ошибок сервиса.
     */
    ChatDetailsDTO createChatWithOperatorFromUI(CreateChatRequestDTO createRequest) throws AccessDeniedException;

    /**
     * Запрашивает эскалацию чата до оператора.
     * Может быть инициировано клиентом, связанным с чатом.
     * Если оператор доступен, чат назначается; в противном случае его статус может измениться на PENDING_OPERATOR.
     *
     * @param chatId   ID чата для эскалации.
     * @param clientId ID клиента, инициирующего эскалацию (для проверки соответствия клиенту чата).
     * @return {@link ChatDetailsDTO} обновленного чата.
     * @throws ChatNotFoundException если чат не найден.
     * @throws ChatServiceException  если предоставленный clientId не соответствует клиенту чата,
     *                               если чат уже закрыт/архивирован, или для других ошибок сервиса.
     */
    ChatDetailsDTO requestOperatorEscalation(Integer chatId, Integer clientId);

    /**
     * Напрямую связывает указанного оператора с чатом.
     * Это действие обычно выполняется менеджером или системным процессом с соответствующими разрешениями.
     * Требует, чтобы чат, оператор и выполняющий пользователь принадлежали одной компании.
     *
     * @param chatId     ID чата.
     * @param operatorId ID оператора для связи с чатом.
     * @throws AccessDeniedException     если у выполняющего пользователя отсутствуют разрешения или если сущности принадлежат разным компаниям.
     * @throws ChatNotFoundException     если чат не найден.
     * @throws ResourceNotFoundException если оператор не найден.
     * @throws ChatServiceException      для других специфичных ошибок сервиса.
     */
    void linkOperatorToChat(Integer chatId, Integer operatorId) throws AccessDeniedException;

    /**
     * Находит первый открытый чат для данного клиента на указанном канале.
     * "Открытые" статусы обычно включают PENDING_AUTO_RESPONDER, PENDING_OPERATOR, ASSIGNED, IN_PROGRESS.
     * Этот метод предназначен в основном для внутренней логики сервиса (например, маршрутизации входящих сообщений)
     * и не подразумевает проверок доступа на основе UserContext.
     *
     * @param clientId ID клиента.
     * @param channel  Канал чата.
     * @return {@link Optional} содержащий сущность {@link Chat}, если открытый чат найден, в противном случае пустой.
     */
    Optional<Chat> findOpenChatByClientAndChannel(Integer clientId, ChatChannel channel);

    /**
     * Находит первый открытый чат для данного клиента, канала и внешнего ID чата.
     * "Открытые" статусы рассматриваются как определено в {@link #findOpenChatByClientAndChannel(Integer, ChatChannel)}.
     * Для внутренней логики сервиса.
     *
     * @param clientId       ID клиента.
     * @param channel        Канал чата.
     * @param externalChatId Внешний идентификатор чата.
     * @return {@link Optional} содержащий сущность {@link Chat}, если открытый чат найден, в противном случае пустой.
     */
    Optional<Chat> findOpenChatByClientAndChannelAndExternalId(Integer clientId, ChatChannel channel, String externalChatId);

    /**
     * Находит сущность чата по его внутреннему ID.
     * Этот метод может использоваться внутренними сервисами.
     * Контроль доступа (если требуется помимо простого существования) должен обрабатываться вызывающим контекстом или,
     * если этот метод фасада подразумевает доступ пользователя, он будет внутренне использовать UserContext.
     *
     * @param chatId ID чата.
     * @return {@link Optional} содержащий сущность {@link Chat}, если найден, в противном случае пустой.
     * @throws AccessDeniedException если текущий пользователь (если контекст применяется неявно) не имеет прав на просмотр этой конкретной сущности чата.
     */
    Optional<Chat> findChatEntityById(Integer chatId) throws AccessDeniedException;

    /**
     * Назначает чат оператору.
     * Если ID оператора указан в запросе, назначает этому конкретному оператору.
     * В противном случае оператор выбирается на основе стратегии назначения (например, наименее загруженный).
     * Требует соответствующих разрешений (например, роль менеджера) и чтобы все сущности находились в одной компании.
     *
     * @param assignRequest DTO, содержащий ID чата и опционально ID оператора.
     * @return {@link ChatDetailsDTO} обновленного чата.
     * @throws AccessDeniedException     если у выполняющего пользователя отсутствуют разрешения, или если чат/оператор принадлежит другой компании.
     * @throws ChatNotFoundException     если чат не найден.
     * @throws ResourceNotFoundException если указанный оператор не найден или нет доступных операторов для автоматического назначения.
     * @throws ChatServiceException      если чат не находится в состоянии, допускающем назначение, или для других ошибок сервиса.
     */
    ChatDetailsDTO assignChat(AssignChatRequestDTO assignRequest) throws AccessDeniedException;

    /**
     * Закрывает чат текущим аутентифицированным пользователем (оператором или менеджером).
     * Статус чата устанавливается в CLOSED.
     *
     * @param chatId ID чата для закрытия.
     * @return {@link ChatDetailsDTO} закрытого чата.
     * @throws AccessDeniedException     если у пользователя нет разрешения на закрытие этого чата.
     * @throws ChatNotFoundException     если чат не найден.
     * @throws ChatServiceException      если чат уже закрыт/архивирован.
     */
    ChatDetailsDTO closeChatByCurrentUser(Integer chatId) throws AccessDeniedException;

    /**
     * Получает подробную информацию о конкретном чате по его ID.
     * Требует, чтобы аутентифицированный пользователь имел разрешение на просмотр чата (например, он принадлежит его компании).
     *
     * @param chatId ID чата.
     * @return {@link ChatDetailsDTO} чата.
     * @throws AccessDeniedException     если у пользователя нет разрешения на просмотр этого чата.
     * @throws ChatNotFoundException     если чат не найден.
     */
    ChatDetailsDTO getChatDetails(Integer chatId) throws AccessDeniedException;

    /**
     * Получает список чатов, доступных для просмотра текущему аутентифицированному пользователю,
     * отфильтрованных по указанным статусам.
     * Для менеджеров это обычно включает чаты их компании (например, PENDING_OPERATOR, ASSIGNED, IN_PROGRESS).
     * Для операторов – их собственные назначенные чаты.
     * Если {@code statuses} равен null или пуст, используются статусы по умолчанию для роли пользователя.
     *
     * @param statuses Набор {@link ChatStatus} для фильтрации.
     * @return Список {@link ChatDTO}.
     * @throws AccessDeniedException если роль пользователя не позволяет просматривать списки чатов.
     */
    List<ChatDTO> getMyChats(Set<ChatStatus> statuses) throws AccessDeniedException;

    /**
     * Получает список чатов, назначенных текущему аутентифицированному пользователю (оператору),
     * отфильтрованных по указанным статусам.
     * Если {@code statuses} равен null или пуст, по умолчанию используются статусы ASSIGNED, IN_PROGRESS.
     *
     * @param statuses Набор {@link ChatStatus} для фильтрации.
     * @return Список {@link ChatDTO}.
     */
    List<ChatDTO> getChatsForCurrentUser(Set<ChatStatus> statuses);

    /**
     * Получает список чатов, назначенных указанному оператору.
     * Доступно только менеджерам в рамках их компании.
     *
     * @param operatorId ID оператора, чьи чаты нужно получить.
     * @param statuses   Набор статусов для фильтрации. Если null или пустой, используются статусы по умолчанию (ASSIGNED, IN_PROGRESS).
     * @return Список {@link ChatDTO}.
     * @throws AccessDeniedException     если у пользователя нет прав или оператор из другой компании.
     * @throws ResourceNotFoundException если оператор с указанным ID не найден.
     */
    List<ChatDTO> getOperatorChats(Integer operatorId, Set<ChatStatus> statuses) throws AccessDeniedException;

    /**
     * Получает список чатов для указанного клиента.
     * Доступно операторам и менеджерам в рамках их компании.
     *
     * @param clientId ID клиента.
     * @return Список {@link ChatDTO}.
     * @throws AccessDeniedException     если у пользователя нет прав или клиент из другой компании.
     * @throws ResourceNotFoundException если клиент с указанным ID не найден.
     */
    List<ChatDTO> getClientChats(Integer clientId) throws AccessDeniedException;

    /**
     * Получает список ожидающих чатов (статус PENDING_OPERATOR) для компании текущего аутентифицированного пользователя.
     * Доступно операторам и менеджерам.
     *
     * @return Список {@link ChatDTO}.
     * @throws AccessDeniedException если у пользователя нет прав или он не привязан к компании.
     */
    List<ChatDTO> getMyCompanyWaitingChats() throws AccessDeniedException;

    /**
     * Обновляет статус чата.
     * Доступно операторам и менеджерам в рамках их компании.
     *
     * @param chatId    ID чата.
     * @param newStatus Новый статус чата.
     * @throws AccessDeniedException если у пользователя нет прав на изменение статуса этого чата.
     * @throws ChatNotFoundException если чат не найден.
     * @throws ChatServiceException  для других ошибок сервиса или невалидных переходов статуса.
     */
    void updateChatStatus(Integer chatId, ChatStatus newStatus) throws AccessDeniedException;

    /**
     * Находит чат по его внешнему ID, ID компании, ID клиента и каналу.
     * Используется для обработки входящих сообщений из внешних систем и не привязан к UserContext.
     *
     * @param companyId      ID компании.
     * @param clientId       ID клиента (может быть не во всех внешних системах).
     * @param channel        Канал чата.
     * @param externalChatId Внешний ID чата из сторонней системы.
     * @return {@link Optional} содержащий сущность {@link Chat}, если найден, в противном случае пустой.
     */
    Optional<Chat> findChatByExternalId(Integer companyId, Integer clientId, ChatChannel channel, String externalChatId);

    /**
     * Отправляет сообщение в чат от лица текущего аутентифицированного оператора/менеджера.
     * Проверяет права доступа к чату и валидность статуса чата для отправки сообщения.
     *
     * @param chatId  ID чата, куда отправляется сообщение.
     * @param content Текст сообщения.
     * @return DTO отправленного сообщения {@link MessageDto}.
     * @throws AccessDeniedException если у пользователя нет прав на отправку сообщения в этот чат.
     * @throws ChatNotFoundException если чат не найден.
     * @throws ChatServiceException  если статус чата не позволяет отправку сообщения или для других ошибок.
     */
    MessageDto sendOperatorMessage(Integer chatId, String content) throws AccessDeniedException;

    /**
     * Создает тестовый чат для текущего аутентифицированного оператора/менеджера.
     * Чат автоматически назначается на текущего пользователя.
     *
     * @return {@link ChatDetailsDTO} созданного тестового чата.
     * @throws AccessDeniedException     если у пользователя нет прав или он не привязан к компании.
     * @throws ResourceNotFoundException если аутентифицированный пользователь не найден.
     */
    ChatDetailsDTO createTestChatForCurrentUser() throws AccessDeniedException;

    /**
     * Помечает сообщения клиента в чате как прочитанные текущим аутентифицированным оператором/менеджером.
     * Проверяет права доступа к чату.
     *
     * @param chatId     ID чата, в котором помечаются сообщения.
     * @param messageIds Коллекция ID сообщений для пометки как прочитанные.
     * @throws AccessDeniedException если у пользователя нет прав на это действие в данном чате.
     * @throws ChatNotFoundException если чат не найден.
     */
    void markClientMessagesAsReadByCurrentUser(Integer chatId, Collection<Integer> messageIds) throws AccessDeniedException;

}

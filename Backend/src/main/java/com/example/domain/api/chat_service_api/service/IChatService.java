package com.example.domain.api.chat_service_api.service;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.domain.api.chat_service_api.model.dto.ChatDTO;
import com.example.domain.api.chat_service_api.model.dto.ChatDetailsDTO;
import com.example.domain.api.chat_service_api.model.rest.chat.AssignChatRequestDTO;
import com.example.domain.api.chat_service_api.model.rest.chat.CloseChatRequestDTO;
import com.example.domain.api.chat_service_api.model.rest.chat.CreateChatRequestDTO;

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


    ChatDetailsDTO createChatWithOperator(CreateChatRequestDTO createRequest);

    /**
     * Назначает оператора на чат.
     * @param assignRequest DTO с ID чата и, возможно, ID оператора.
     * @return Обновленный ChatDetailsDTO.
     */
    ChatDetailsDTO assignOperatorToChat(AssignChatRequestDTO assignRequest);

    /**
     * Закрывает чат.
     * @param closeRequest DTO с ID чата.
     * @return Обновленный ChatDetailsDTO.
     */
    ChatDetailsDTO closeChat(CloseChatRequestDTO closeRequest);

    /**
     * Получает детальную информацию о чате по его ID.
     * @param chatId ID чата.
     * @return ChatDetailsDTO.
     */
    ChatDetailsDTO getChatDetails(Integer chatId);

    /**
     * Получает список чатов для оператора (например, открытые чаты).
     * @param userId ID оператора.
     * @return Список ChatDTO.
     */
    List<ChatDTO> getOperatorChats(Integer userId);

    List<ChatDTO> getOperatorChatsStatus(Integer userId, Set<ChatStatus> statuses);

    /**
     * Получает список чатов для клиента.
     * @param clientId ID клиента.
     * @return Список ChatDTO.
     */
    List<ChatDTO> getClientChats(Integer clientId);

    ChatDetailsDTO requestOperatorEscalation(Integer chatId, Integer clientId);

    Optional<Chat> findOpenChatByClientAndChannel(Integer clientId, ChatChannel channel);

    Optional<Chat> findOpenChatByClient(Integer clientId);

    Optional<Chat> findChatEntityById(Integer chatId);

    /**
     * Получает список чатов, которые ждут назначения оператора (для панели операторов).
     * @param companyId ID компании.
     * @return Список ChatDTO.
     */
    List<ChatDTO> getWaitingChats(Integer companyId);

    void updateChatStatus(Integer chatId, ChatStatus newStatus);

    void linkOperatorToChat(Integer chatId, Integer operatorId);

    Optional<Chat> findChatByExternalId(Integer companyId, Integer clientId, ChatChannel channel, String externalChatId);
}

package com.example.database.repository.chats_messages_module;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.database.model.chats_messages_module.message.MessageStatus;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {

    List<ChatMessage> findByChatIdOrderBySentAtAsc(Integer chatId);

    Optional<ChatMessage> findByIdAndExternalMessageId(Integer id, String externalMessageId);

    Optional<ChatMessage> findFirstByChatIdOrderBySentAtAsc(Integer chatId);

    @Query("SELECT DISTINCT m.chat " +
            "FROM ChatMessage m " +
            "WHERE m.chat.client.id = :clientId " +
            "AND m.chat.chatChannel = :channel " +
            "AND m.chat.status IN (:statuses) " +
            "ORDER BY m.chat.createdAt DESC")
    Optional<Chat> findFirstChatByChatClient_IdAndChatChannelAndChatStatusInOrderByChatCreatedAtDesc(
            @Param("clientId") Integer clientId,
            @Param("channel") ChatChannel channel,
            @Param("statuses") Collection<ChatStatus> statuses
    );

    List<ChatMessage> findAllByIdInAndChatIdAndSenderTypeAndStatusNot(
            Collection<Integer> messageIds,
            Integer chatId,
            ChatMessageSenderType senderType,
            MessageStatus excludedStatus
    );

    Optional<ChatMessage> findByChatIdAndExternalMessageIdAndSenderType(
            Integer chatId,
            String externalMessageId,
            ChatMessageSenderType senderType
    );

}

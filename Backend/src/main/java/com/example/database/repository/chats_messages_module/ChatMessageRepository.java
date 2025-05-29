package com.example.database.repository.chats_messages_module;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.database.model.chats_messages_module.message.MessageStatus;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
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

    Optional<ChatMessage> findFirstByChatIdOrderBySentAtAsc(Integer chatId);

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

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chat.id = :chatId " +
            "AND cm.senderType = :senderType " +
            "AND (:excludeId IS NULL OR cm.id <> :excludeId) " +
            "ORDER BY cm.sentAt DESC")
    List<ChatMessage> findLastNClientMessagesExcludingId(
            @Param("chatId") Integer chatId,
            @Param("senderType") ChatMessageSenderType senderType,
            @Param("excludeId") Integer excludeId,
            Pageable pageable
    );

}

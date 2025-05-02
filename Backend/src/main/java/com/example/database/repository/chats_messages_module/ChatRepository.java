package com.example.database.repository.chats_messages_module;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.crm_module.client.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Integer> {

    Optional<Chat> findByClientAndChatChannel(Client client, ChatChannel chatChannel);

    @Query("SELECT c FROM Chat c WHERE c.client.id = :clientId AND c.chatChannel = :channel AND c.id = :id AND c.company.id = :companyId")
    Optional<Chat> findByClientIdAndChatChannelAndId(@Param("companyId") Integer companyId,
                                                                 @Param("clientId") Integer clientId,
                                                                 @Param("channel") ChatChannel channel,
                                                                 @Param("id") String externalChatId);

    Optional<Chat> findByClientIdAndChatChannel(Integer clientId, ChatChannel channel);

    List<Chat> findByUserIdAndStatusIn(Integer userId, Collection<ChatStatus> statuses);

    List<Chat> findByCompanyIdAndStatusOrderByLastMessageAtDesc(Integer companyId, ChatStatus status);

    List<Chat> findByCompanyIdAndStatusInOrderByLastMessageAtDesc(Integer companyId, Collection<ChatStatus> openStatuses);

    boolean existsByIdAndUserId(Integer chatId, Integer userId);

    Optional<Chat> findFirstByClientIdAndChatChannelAndStatusInOrderByCreatedAtDesc(Integer clientId, ChatChannel channel, Collection<ChatStatus> statuses);

    Optional<Chat> findByClientId(Integer clientId);

    @Modifying
    @Query("UPDATE Chat c SET c.status = :status, c.assignedAt = :assignedAt WHERE c.id = :chatId")
    int updateStatus(@Param("chatId") Integer chatId,
                     @Param("status") ChatStatus status,
                     @Param("assignedAt") LocalDateTime assignedAt);
}

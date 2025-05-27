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
import java.util.Set;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Integer> {

    List<Chat> findByUserIdAndStatusIn(Integer userId, Collection<ChatStatus> statuses);

    List<Chat> findByCompanyIdAndStatusOrderByLastMessageAtDesc(Integer companyId, ChatStatus status);

    List<Chat> findByCompanyIdAndStatusInOrderByLastMessageAtDesc(Integer companyId, Collection<ChatStatus> openStatuses);

    List<Chat> findByClientIdAndCompanyIdOrderByCreatedAtDesc(Integer clientId, Integer companyId);

    Optional<Chat> findFirstByClientIdAndChatChannelAndStatusInOrderByCreatedAtDesc(Integer clientId, ChatChannel channel, Collection<ChatStatus> statuses);

    Optional<Chat> findFirstByClientIdAndCompanyIdAndChatChannelAndStatusInOrderByCreatedAtDesc(Integer clientId, Integer companyId, ChatChannel chatChannel, Collection<ChatStatus> statuses);

    Optional<Chat> findFirstByClientIdAndChatChannelAndExternalChatIdAndStatusInOrderByCreatedAtDesc(
            Integer clientId,
            ChatChannel chatChannel,
            String externalChatId,
            Collection<ChatStatus> statuses
    );

    @Query("SELECT c.user.id as userId, COUNT(c.id) as chatCount " +
            "FROM Chat c " +
            "WHERE c.user.id IN :userIds AND c.status IN :statuses " +
            "GROUP BY c.user.id")
    List<OperatorChatCount> countChatsByUserIdInAndStatusIn(@Param("userIds") List<Integer> userIds,
                                                            @Param("statuses") Set<ChatStatus> statuses);

    interface OperatorChatCount {
        Integer getUserId();
        Long getChatCount();
    }
}

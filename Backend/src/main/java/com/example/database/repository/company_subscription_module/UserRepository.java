package com.example.database.repository.company_subscription_module;

import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.domain.dto.UserCompanyRolesDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    @Query("SELECT u FROM User u WHERE u.company.id = :companyId ORDER BY SIZE(u.clients) ASC")
    Optional<User> findLeastBusyUser(@Param("companyId") Integer companyId);

    @Query("SELECT u FROM User u WHERE u.company.id = :companyId " +
            "ORDER BY (SELECT COUNT(c) FROM Chat c WHERE c.user = u AND c.status IN :openStatuses) ASC NULLS FIRST, " +
            "u.id ASC")
    List<User> findLeastBusyUser(@Param("companyId") Integer companyId,
                                 @Param("openStatuses") Collection<ChatStatus> openStatuses);

    @Query("SELECT COUNT(c) FROM Chat c WHERE c.user.id = :userId AND c.status IN :openStatuses")
    long countOpenChatsByUserId(@Param("userId") Integer userId,
                                @Param("openStatuses") Collection<ChatStatus> openStatuses);

    Optional<User> findByEmail(String email);

    Optional<User> findByCompanyId(Integer companyId);

    @Query("SELECT UserCompanyRolesDto(u, u.company) " +
            "FROM User u WHERE u.email = :email")
    Optional<UserCompanyRolesDto> findUserData(@Param("email") String email);
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.company.id = :companyId WHERE u.email = :email")
    void updateByCompanyIdAndEmail(@Param("companyId") Integer companyId,
                                   @Param("email") String email);

    List<User> getAllByCompanyId(Integer companyId);
}

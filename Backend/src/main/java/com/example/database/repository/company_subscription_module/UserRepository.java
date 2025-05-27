package com.example.database.repository.company_subscription_module;

import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.model.company_subscription_module.user_roles.user.User;

import com.example.database.model.company_subscription_module.user_roles.user.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    @Query("SELECT u FROM User u JOIN UserRole ur ON ur.user = u " +
            "WHERE u.company.id = :companyId AND ur.role = :roleName AND u.status IN :statuses")
    List<User> findByCompanyIdAndRoleAndStatusIn(@Param("companyId") Integer companyId,
                                                 @Param("roleName") Role roleName,
                                                 @Param("statuses") Set<UserStatus> statuses);

    Optional<User> findByEmail(String email);

    List<User> findByCompanyId(Integer companyId);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.company.id = :companyId WHERE u.email = :email")
    void updateByCompanyIdAndEmail(@Param("companyId") Integer companyId,
                                   @Param("email") String email);

    List<User> getAllByCompanyId(Integer companyId);
}

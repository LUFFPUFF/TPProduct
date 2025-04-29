package com.example.database.repository.company_subscription_module;

import com.example.database.model.company_subscription_module.user_roles.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    @Query("SELECT u FROM User u WHERE u.company.id = :companyId ORDER BY SIZE(u.clients) ASC")
    Optional<User> findLeastBusyUser(@Param("companyId") Integer companyId);
}

package com.example.database.repository.company_subscription_module;

import com.example.database.model.company_subscription_module.user_roles.UserRole;
import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Integer> {
    @Query("SELECT role FROM UserRole WHERE user.email = :email")
    List<Role> findRolesByEmail(@Param("email") String email);
    void deleteByUserAndRole(User user, Role role);
}

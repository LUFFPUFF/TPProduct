package com.example.database.repository.company_subscription_module;

import com.example.database.model.company_subscription_module.user_roles.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Integer> {
}

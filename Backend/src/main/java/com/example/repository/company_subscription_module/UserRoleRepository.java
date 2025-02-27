package com.example.repository.company_subscription_module;

import com.example.model.company_subscription_module.user_roles.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, Integer> {
}

package com.example.database.repository.company_subscription_module;

import com.example.database.model.company_subscription_module.Company;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.company = :company WHERE u.email = :email")
    void updateCompanyByEmail(@Param("email") String email, @Param("company") Company company);
    List<User> getUsersByCompany(Company company);

    @Query("SELECT u FROM User u WHERE u.company.id = :companyId ORDER BY SIZE(u.clients) ASC")
    Optional<User> findLeastBusyUser(@Param("companyId") Integer companyId);
}

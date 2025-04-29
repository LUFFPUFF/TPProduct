package com.example.database.repository.company_subscription_module;

import com.example.database.model.company_subscription_module.Company;
import com.example.database.model.company_subscription_module.subscription.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Integer> {
    Optional<Subscription> findByCompany(Company company);
}

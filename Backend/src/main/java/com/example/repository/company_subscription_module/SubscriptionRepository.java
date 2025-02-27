package com.example.repository.company_subscription_module;

import com.example.model.company_subscription_module.subscription.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Integer> {
}

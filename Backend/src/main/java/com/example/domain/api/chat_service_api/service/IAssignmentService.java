package com.example.domain.api.chat_service_api.service;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.company_subscription_module.user_roles.user.User;

import java.util.Optional;

public interface IAssignmentService {

    /**
     * Находит наименее загруженного оператора для компании.
     * @param companyId ID компании.
     * @return Optional User Entity - найденный оператор.
     */
    Optional<User> findLeastBusyOperator(Integer companyId);

    /**
     * Назначает оператора на чат согласно стратегии.
     * Может использовать findLeastBusyOperator или другую логику.
     * @param chat Чат, на который нужно назначить оператора.
     * @return Optional User Entity - назначенный оператор.
     */
    Optional<User> assignOperator(Chat chat);
}

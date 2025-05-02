package com.example.domain.api.chat_service_api.service;

import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.domain.dto.UserDto;

import java.util.List;
import java.util.Optional;

public interface IUserService {

    /**
     * Находит пользователя по ID.
     * @param userId ID пользователя.
     * @return Optional User Entity.
     */
    Optional<User> findById(Integer userId);

    /**
     * Находит пользователя по ID и возвращает DTO.
     * @param userId ID пользователя.
     * @return Optional UserDTO.
     */
    Optional<UserDto> findDtoById(Integer userId);

     List<UserDto> getAllUsers(Integer companyId);
     void updateOnlineStatus(Integer userId, boolean isOnline);
}

package com.example.domain.api.company_module.service;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.dto.UserDto;

import java.nio.file.AccessDeniedException;
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


    Optional<User> findByEmail(String name);

     List<UserDto> getAllUsers(Integer companyId);

     void updateOnlineStatus(Integer userId, boolean isOnline);

     Company findUserCompanyOrThrow(Integer userId) throws ResourceNotFoundException, AccessDeniedException;
}

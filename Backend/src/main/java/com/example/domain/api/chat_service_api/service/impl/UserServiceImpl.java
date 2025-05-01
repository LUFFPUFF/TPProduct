package com.example.domain.api.chat_service_api.service.impl;

import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.chat_service_api.mapper.UserMapper;
import com.example.domain.api.chat_service_api.model.dto.user.UserDTO;
import com.example.domain.api.chat_service_api.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public Optional<User> findById(Integer userId) {
        return userRepository.findById(userId);
    }

    @Override
    @Transactional
    public Optional<UserDTO> findDtoById(Integer userId) {
        return findById(userId)
                .map(userMapper::toDto);
    }

    @Override
    @Transactional
    public List<UserDTO> getAllUsers(Integer companyId) {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Override
    public void updateOnlineStatus(Integer userId, boolean isOnline) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

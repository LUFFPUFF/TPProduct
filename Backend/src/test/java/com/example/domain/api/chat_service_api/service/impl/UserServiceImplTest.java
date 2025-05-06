package com.example.domain.api.chat_service_api.service.impl;

import com.example.database.model.company_subscription_module.company.Company; // Нужен для User
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.chat_service_api.mapper.UserMapper;
import com.example.domain.api.chat_service_api.model.dto.user.UserInfoDTO; // Нужен для маппера
import com.example.domain.dto.CompanyDto; // Нужен для UserDto
import com.example.domain.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataAccessException; // Для ошибки репозитория

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser1;
    private User testUser2; // Для теста getAllUsers
    private UserDto testUserDto1;
    private UserDto testUserDto2; // Для теста getAllUsers
    private UserInfoDTO testUserInfoDto; // Не используется напрямую в тестах, но нужен для маппера
    private final Integer USER_ID_1 = 1;
    private final Integer USER_ID_2 = 2;
    private final Integer COMPANY_ID_IRRELEVANT = 99; // ID компании, который будет игнорироваться


    @BeforeEach
    void setUp() {
        reset(userRepository, userMapper);

        // Создаем Company для User
        Company company1 = new Company();
        company1.setId(1);
        Company company2 = new Company();
        company2.setId(2);

        testUser1 = new User();
        testUser1.setId(USER_ID_1);
        testUser1.setFullName("Test User One");
        testUser1.setEmail("user1@test.com");
        testUser1.setCompany(company1); // Привязываем к компании

        testUser2 = new User(); // Второй пользователь для findAll
        testUser2.setId(USER_ID_2);
        testUser2.setFullName("Test User Two");
        testUser2.setEmail("user2@test.com");
        testUser2.setCompany(company2); // Другая компания

        // Создаем CompanyDto для UserDto
        CompanyDto companyDto1 = new CompanyDto();
        companyDto1.setId(1);
        CompanyDto companyDto2 = new CompanyDto();
        companyDto2.setId(2);


        testUserDto1 = new UserDto();
        testUserDto1.setId(USER_ID_1);
        testUserDto1.setFullName("Test User One");
        testUserDto1.setCompanyDto(companyDto1); // Устанавливаем DTO компании

        testUserDto2 = new UserDto();
        testUserDto2.setId(USER_ID_2);
        testUserDto2.setFullName("Test User Two");
        testUserDto2.setCompanyDto(companyDto2);

        // UserInfoDTO (не используется в ассертах, но нужен для компиляции)
        testUserInfoDto = new UserInfoDTO();
        testUserInfoDto.setId(USER_ID_1);
        testUserInfoDto.setFullName("Test User One");
    }

    // --- Тесты для findById ---

    @Test
    void findById_WhenUserExists_ShouldReturnUser() {
        when(userRepository.findById(USER_ID_1)).thenReturn(Optional.of(testUser1));
        Optional<User> result = userService.findById(USER_ID_1);
        assertTrue(result.isPresent());
        assertEquals(testUser1, result.get());
        verify(userRepository).findById(USER_ID_1);
    }

    @Test
    void findById_WhenUserNotExists_ShouldReturnEmpty() {
        when(userRepository.findById(eq(USER_ID_2 + 1))).thenReturn(Optional.empty());
        Optional<User> result = userService.findById(USER_ID_2 + 1);
        assertTrue(result.isEmpty());
        verify(userRepository).findById(USER_ID_2 + 1);
    }

    @Test
    void findById_RepositoryThrowsException_ShouldThrowException() {
        DataAccessException dbException = new DataAccessException("DB Error") {};
        when(userRepository.findById(USER_ID_1)).thenThrow(dbException);

        DataAccessException thrown = assertThrows(DataAccessException.class, () -> {
            userService.findById(USER_ID_1);
        });
        assertEquals(dbException, thrown);
        verify(userRepository).findById(USER_ID_1);
    }


    // --- Тесты для findDtoById ---

    @Test
    void findDtoById_WhenUserExists_ShouldReturnUserDto() {
        when(userRepository.findById(USER_ID_1)).thenReturn(Optional.of(testUser1));
        when(userMapper.toDto(testUser1)).thenReturn(testUserDto1);
        Optional<UserDto> result = userService.findDtoById(USER_ID_1);
        assertTrue(result.isPresent());
        assertEquals(testUserDto1, result.get());
        verify(userRepository).findById(USER_ID_1);
        verify(userMapper).toDto(testUser1);
    }

    @Test
    void findDtoById_WhenUserNotExists_ShouldReturnEmpty() {
        when(userRepository.findById(eq(USER_ID_2 + 1))).thenReturn(Optional.empty());
        Optional<UserDto> result = userService.findDtoById(USER_ID_2 + 1);
        assertTrue(result.isEmpty());
        verify(userRepository).findById(USER_ID_2 + 1);
        verify(userMapper, never()).toDto(any());
    }

    @Test
    void findDtoById_RepositoryThrowsException_ShouldThrowException() {
        DataAccessException dbException = new DataAccessException("DB Error") {};
        when(userRepository.findById(USER_ID_1)).thenThrow(dbException);

        DataAccessException thrown = assertThrows(DataAccessException.class, () -> {
            userService.findDtoById(USER_ID_1);
        });
        assertEquals(dbException, thrown);
        verify(userRepository).findById(USER_ID_1);
        verifyNoInteractions(userMapper);
    }

    @Test
    void findDtoById_MapperThrowsException_ShouldThrowException() {
        when(userRepository.findById(USER_ID_1)).thenReturn(Optional.of(testUser1));
        RuntimeException mapperException = new RuntimeException("Mapping failed");
        when(userMapper.toDto(testUser1)).thenThrow(mapperException);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            userService.findDtoById(USER_ID_1);
        });
        assertEquals(mapperException, thrown);
        verify(userRepository).findById(USER_ID_1);
        verify(userMapper).toDto(testUser1);
    }


    // --- Тесты для getAllUsers ---

    @Test
        // ИСПРАВЛЕНО: Проверяем вызов findAll и возврат ВСЕХ пользователей
    void getAllUsers_ShouldCallFindAllAndReturnAllUserDtos() {
        // Arrange
        List<User> allUsers = List.of(testUser1, testUser2);
        when(userRepository.findAll()).thenReturn(allUsers); // Мокируем findAll()
        when(userMapper.toDto(testUser1)).thenReturn(testUserDto1);
        when(userMapper.toDto(testUser2)).thenReturn(testUserDto2);

        // Act
        // Передаем ID компании, но ожидаем, что он будет проигнорирован
        List<UserDto> result = userService.getAllUsers(COMPANY_ID_IRRELEVANT);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size()); // Ожидаем ВСЕХ пользователей
        assertTrue(result.contains(testUserDto1));
        assertTrue(result.contains(testUserDto2));
        verify(userRepository).findAll(); // Проверяем вызов findAll()
        verify(userRepository, never()).getAllByCompanyId(anyInt()); // Убедимся, что getAllByCompanyId не вызывался
        verify(userMapper, times(2)).toDto(any(User.class));
    }

    @Test
        // ДОБАВЛЕНО: Тест на случай отсутствия пользователей
    void getAllUsers_WhenNoUsers_ShouldReturnEmptyList() {
        // Arrange
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<UserDto> result = userService.getAllUsers(COMPANY_ID_IRRELEVANT);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository).findAll();
        verifyNoInteractions(userMapper);
    }

    @Test
        // ДОБАВЛЕНО: Тест на ошибку репозитория
    void getAllUsers_RepositoryThrowsException_ShouldThrowException() {
        // Arrange
        DataAccessException dbException = new DataAccessException("DB Error") {};
        when(userRepository.findAll()).thenThrow(dbException);

        // Act & Assert
        DataAccessException thrown = assertThrows(DataAccessException.class, () -> {
            userService.getAllUsers(COMPANY_ID_IRRELEVANT);
        });
        assertEquals(dbException, thrown);
        verify(userRepository).findAll();
        verifyNoInteractions(userMapper);
    }

    @Test
        // ДОБАВЛЕНО: Тест на ошибку маппера
    void getAllUsers_MapperThrowsException_ShouldThrowException() {
        // Arrange
        List<User> allUsers = List.of(testUser1);
        when(userRepository.findAll()).thenReturn(allUsers);
        RuntimeException mapperException = new RuntimeException("Mapping failed");
        when(userMapper.toDto(testUser1)).thenThrow(mapperException);

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            userService.getAllUsers(COMPANY_ID_IRRELEVANT);
        });
        assertEquals(mapperException, thrown);
        verify(userRepository).findAll();
        verify(userMapper).toDto(testUser1);
    }

    // --- Тест для updateOnlineStatus ---

    @Test
        // ДОБАВЛЕНО: Тест для нереализованного метода
    void updateOnlineStatus_ShouldThrowUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> {
            userService.updateOnlineStatus(USER_ID_1, true);
        });
    }

}
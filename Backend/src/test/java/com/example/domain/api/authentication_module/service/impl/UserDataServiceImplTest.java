package com.example.domain.api.authentication_module.service.impl;

import com.example.database.model.company_subscription_module.company.Company; // Для UserCompanyRolesDto
import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.model.company_subscription_module.user_roles.user.User; // Для UserCompanyRolesDto
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.authentication_module.exception_handler_auth.NotFoundUserException;
import com.example.domain.api.authentication_module.service.interfaces.RoleService;
// import com.example.domain.api.authentication_module.service.interfaces.UserDataService; // Не нужен для теста реализации
import com.example.domain.dto.UserCompanyRolesDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataAccessException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserDataServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleService roleService;

    @InjectMocks
    private UserDataServiceImpl userDataService;

    private UserCompanyRolesDto testUserCompanyRolesDto;
    private User testUser; // Для конструктора UserCompanyRolesDto
    private Company testCompany; // Для конструктора UserCompanyRolesDto
    private List<Role> testRoles;

    private final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        reset(userRepository, roleService);

        testUser = new User(); // Фиктивный User
        testUser.setEmail(TEST_EMAIL);
        testUser.setId(1);

        testCompany = new Company(); // Фиктивная Company
        testCompany.setId(1);

        // Создаем UserCompanyRolesDto так, как он может прийти из репозитория
        // Важно: конструктор UserCompanyRolesDto(User user, Company company, List<Role> userRoles)
        // В реальном вызове userRepository.findUserData, поле userRoles изначально будет null
        testUserCompanyRolesDto = UserCompanyRolesDto.builder()
                .user(testUser)
                .company(testCompany)
                // userRoles будет установлен позже из roleService
                .build();

        testRoles = List.of(Role.OPERATOR, Role.MANAGER);
    }

    @Test
    void getUserData_UserExistsAndHasRoles_ShouldReturnDtoWithRoles() {
        // Arrange
        when(userRepository.findUserData(TEST_EMAIL)).thenReturn(Optional.of(testUserCompanyRolesDto));
        when(roleService.getUserRoles(TEST_EMAIL)).thenReturn(testRoles);

        // Act
        UserCompanyRolesDto result = userDataService.getUserData(TEST_EMAIL);

        // Assert
        assertNotNull(result);
        assertEquals(testUser, result.getUser());
        assertEquals(testCompany, result.getCompany());
        assertEquals(testRoles, result.getUserRoles()); // Проверяем, что роли установились

        verify(userRepository).findUserData(TEST_EMAIL);
        verify(roleService).getUserRoles(TEST_EMAIL);
    }

    @Test
    void getUserData_UserNotFound_ShouldThrowNotFoundUserException() {
        // Arrange
        when(userRepository.findUserData(TEST_EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundUserException.class, () -> {
            userDataService.getUserData(TEST_EMAIL);
        });

        verify(userRepository).findUserData(TEST_EMAIL);
        verifyNoInteractions(roleService); // roleService не должен вызываться
    }

    @Test
    void getUserData_UserExistsButHasNoRoles_ShouldReturnDtoWithEmptyRoleList() {
        // Arrange
        when(userRepository.findUserData(TEST_EMAIL)).thenReturn(Optional.of(testUserCompanyRolesDto));
        when(roleService.getUserRoles(TEST_EMAIL)).thenReturn(Collections.emptyList()); // Нет ролей

        // Act
        UserCompanyRolesDto result = userDataService.getUserData(TEST_EMAIL);

        // Assert
        assertNotNull(result);
        assertEquals(testUser, result.getUser());
        assertTrue(result.getUserRoles().isEmpty()); // Список ролей должен быть пустым

        verify(userRepository).findUserData(TEST_EMAIL);
        verify(roleService).getUserRoles(TEST_EMAIL);
    }

    @Test
    void getUserData_UserRepositoryThrowsException_ShouldThrowException() {
        // Arrange
        DataAccessException dbException = new DataAccessException("DB Error finding user data") {};
        when(userRepository.findUserData(TEST_EMAIL)).thenThrow(dbException);

        // Act & Assert
        DataAccessException thrown = assertThrows(DataAccessException.class, () -> {
            userDataService.getUserData(TEST_EMAIL);
        });
        assertEquals(dbException, thrown); // Ожидаем проброс исходного исключения

        verify(userRepository).findUserData(TEST_EMAIL);
        verifyNoInteractions(roleService);
    }

    @Test
    void getUserData_RoleServiceThrowsException_ShouldThrowException() {
        // Arrange
        when(userRepository.findUserData(TEST_EMAIL)).thenReturn(Optional.of(testUserCompanyRolesDto));
        RuntimeException roleServiceException = new RuntimeException("Role service error");
        when(roleService.getUserRoles(TEST_EMAIL)).thenThrow(roleServiceException);

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            userDataService.getUserData(TEST_EMAIL);
        });
        assertEquals(roleServiceException, thrown); // Ожидаем проброс исходного исключения

        verify(userRepository).findUserData(TEST_EMAIL);
        verify(roleService).getUserRoles(TEST_EMAIL);
    }
}
package com.example.domain.api.authentication_module.service.impl;

import com.example.database.model.company_subscription_module.user_roles.UserRole;
import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.database.repository.company_subscription_module.UserRoleRepository;
import com.example.domain.api.authentication_module.cache.AuthCacheService;
import com.example.domain.api.authentication_module.exception_handler_auth.NotFoundUserException;
// import com.example.domain.api.authentication_module.service.interfaces.RoleService; // Не нужен в тесте
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataAccessException; // Для ошибок репозитория

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoleServiceImplTest {

    @Mock private UserRoleRepository userRoleRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthCacheService authCacheService;

    @InjectMocks
    private RoleServiceImpl roleService;

    @Captor private ArgumentCaptor<UserRole> userRoleCaptor;
    @Captor private ArgumentCaptor<String> stringCaptor;

    private User testUser;
    private final String TEST_EMAIL = "test@example.com";
    private final Integer USER_ID = 1;

    @BeforeEach
    void setUp() {
        reset(userRoleRepository, userRepository, authCacheService);

        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setEmail(TEST_EMAIL);
    }

    // --- Тесты для addRole ---

    @Test
    void addRole_UserExists_ShouldSaveUserRoleAndReturnTrue() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(authCacheService).putExpiredData(anyString());

        // Act
        boolean result = roleService.addRole(TEST_EMAIL, Role.OPERATOR);

        // Assert
        assertTrue(result);
        verify(authCacheService).putExpiredData(TEST_EMAIL);
        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(userRoleRepository).save(userRoleCaptor.capture());
        UserRole savedUserRole = userRoleCaptor.getValue();
        assertEquals(testUser, savedUserRole.getUser());
        assertEquals(Role.OPERATOR, savedUserRole.getRole());
    }

    @Test
    void addRole_UserNotFound_ShouldThrowNotFoundUserException() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
        doNothing().when(authCacheService).putExpiredData(anyString());

        // Act & Assert
        assertThrows(NotFoundUserException.class, () -> {
            roleService.addRole(TEST_EMAIL, Role.MANAGER);
        });
        verify(authCacheService).putExpiredData(TEST_EMAIL);
        verify(userRepository).findByEmail(TEST_EMAIL);
        verifyNoInteractions(userRoleRepository); // save не должен вызываться
    }

    @Test
    void addRole_RepositorySaveFails_ShouldThrowException() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        DataAccessException dbException = new DataAccessException("Save failed") {};
        when(userRoleRepository.save(any(UserRole.class))).thenThrow(dbException);
        doNothing().when(authCacheService).putExpiredData(anyString());


        // Act & Assert
        DataAccessException thrown = assertThrows(DataAccessException.class, () -> {
            roleService.addRole(TEST_EMAIL, Role.OPERATOR);
        });
        assertEquals(dbException, thrown); // Ожидаем проброс исходного исключения
        verify(authCacheService).putExpiredData(TEST_EMAIL);
        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(userRoleRepository).save(any(UserRole.class));
    }

    @Test
    void addRole_AuthCacheFails_ShouldStillAttemptUserOperationsAndSucceed() {
        // Arrange
        // Мокируем ошибку кеша, но остальные операции должны пройти
        doThrow(new RuntimeException("Cache error")).when(authCacheService).putExpiredData(anyString());
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));


        // Act & Assert
        // Ожидаем, что основная логика добавления роли выполнится, несмотря на ошибку кеша
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            roleService.addRole(TEST_EMAIL, Role.OPERATOR);
        });
        assertEquals("Cache error", thrown.getMessage());

        // Важно: userRepository и userRoleRepository НЕ должны были быть вызваны, так как ошибка кеша произошла раньше
        verify(authCacheService).putExpiredData(TEST_EMAIL);
        verifyNoInteractions(userRepository, userRoleRepository);
    }


    // --- Тесты для getUserRoles ---

    @Test
    void getUserRoles_UserHasRoles_ShouldReturnRoleList() {
        // Arrange
        List<Role> expectedRoles = List.of(Role.OPERATOR, Role.MANAGER);
        when(userRoleRepository.findRolesByEmail(TEST_EMAIL)).thenReturn(expectedRoles);

        // Act
        List<Role> actualRoles = roleService.getUserRoles(TEST_EMAIL);

        // Assert
        assertEquals(expectedRoles, actualRoles);
        verify(userRoleRepository).findRolesByEmail(TEST_EMAIL);
    }

    @Test
    void getUserRoles_UserHasNoRoles_ShouldReturnEmptyList() {
        // Arrange
        when(userRoleRepository.findRolesByEmail(TEST_EMAIL)).thenReturn(Collections.emptyList());

        // Act
        List<Role> actualRoles = roleService.getUserRoles(TEST_EMAIL);

        // Assert
        assertTrue(actualRoles.isEmpty());
        verify(userRoleRepository).findRolesByEmail(TEST_EMAIL);
    }

    @Test
    void getUserRoles_RepositoryFails_ShouldThrowException() {
        // Arrange
        DataAccessException dbException = new DataAccessException("Find roles failed") {};
        when(userRoleRepository.findRolesByEmail(TEST_EMAIL)).thenThrow(dbException);

        // Act & Assert
        DataAccessException thrown = assertThrows(DataAccessException.class, () -> {
            roleService.getUserRoles(TEST_EMAIL);
        });
        assertEquals(dbException, thrown);
        verify(userRoleRepository).findRolesByEmail(TEST_EMAIL);
    }

    // --- Тесты для removeRole ---

    @Test
    void removeRole_UserAndRoleExist_ShouldCallDeleteAndReturnTrue() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        doNothing().when(userRoleRepository).deleteByUserAndRole(testUser, Role.OPERATOR);
        doNothing().when(authCacheService).putExpiredData(anyString());

        // Act
        boolean result = roleService.removeRole(TEST_EMAIL, Role.OPERATOR);

        // Assert
        assertTrue(result);
        verify(authCacheService).putExpiredData(TEST_EMAIL);
        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(userRoleRepository).deleteByUserAndRole(testUser, Role.OPERATOR);
    }

    @Test
    void removeRole_UserNotFound_ShouldThrowNotFoundUserException() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
        doNothing().when(authCacheService).putExpiredData(anyString());

        // Act & Assert
        assertThrows(NotFoundUserException.class, () -> {
            roleService.removeRole(TEST_EMAIL, Role.MANAGER);
        });
        verify(authCacheService).putExpiredData(TEST_EMAIL);
        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(userRoleRepository, never()).deleteByUserAndRole(any(), any());
    }

    @Test
    void removeRole_RoleNotFoundForUser_ShouldStillCallDeleteAndReturnTrue() {
        // Arrange
        // deleteByUserAndRole (void) не бросает исключение, если запись не найдена
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        doNothing().when(userRoleRepository).deleteByUserAndRole(testUser, Role.OPERATOR); // Предполагаем, что удаление несуществующей роли не вызывает ошибку
        doNothing().when(authCacheService).putExpiredData(anyString());

        // Act
        boolean result = roleService.removeRole(TEST_EMAIL, Role.OPERATOR);

        // Assert
        assertTrue(result); // Сервис возвращает true, даже если ничего не удалено
        verify(authCacheService).putExpiredData(TEST_EMAIL);
        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(userRoleRepository).deleteByUserAndRole(testUser, Role.OPERATOR);
    }

    @Test
    void removeRole_RepositoryDeleteFails_ShouldThrowException() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        DataAccessException dbException = new DataAccessException("Delete failed") {};
        doThrow(dbException).when(userRoleRepository).deleteByUserAndRole(testUser, Role.OPERATOR);
        doNothing().when(authCacheService).putExpiredData(anyString());

        // Act & Assert
        DataAccessException thrown = assertThrows(DataAccessException.class, () -> {
            roleService.removeRole(TEST_EMAIL, Role.OPERATOR);
        });
        assertEquals(dbException, thrown);
        verify(authCacheService).putExpiredData(TEST_EMAIL);
        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(userRoleRepository).deleteByUserAndRole(testUser, Role.OPERATOR);
    }

    @Test
    void removeRole_AuthCacheFails_ShouldStillAttemptUserOperationsAndSucceed() {
        // Arrange
        doThrow(new RuntimeException("Cache error")).when(authCacheService).putExpiredData(anyString());
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        doNothing().when(userRoleRepository).deleteByUserAndRole(testUser, Role.OPERATOR);

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            roleService.removeRole(TEST_EMAIL, Role.OPERATOR);
        });
        assertEquals("Cache error", thrown.getMessage());

        verify(authCacheService).putExpiredData(TEST_EMAIL);
        verifyNoInteractions(userRepository, userRoleRepository);
    }
}
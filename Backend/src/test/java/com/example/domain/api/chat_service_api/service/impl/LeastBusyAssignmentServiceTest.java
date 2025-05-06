package com.example.domain.api.chat_service_api.service.impl;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataAccessException; // Пример исключения репозитория

import java.util.Collection; // Используем Collection для аргумента
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*; // Используем any() для коллекции статусов
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LeastBusyAssignmentServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LeastBusyAssignmentService assignmentService;

    private User testOperator1;
    private User testOperator2;
    // ИСПРАВЛЕНО: Используем Collection<ChatStatus>, как в сигнатуре метода репозитория
    private Collection<ChatStatus> openStatuses;
    private final Integer COMPANY_ID = 1;

    @BeforeEach
    void setUp() {
        reset(userRepository); // Сброс мока

        testOperator1 = new User();
        testOperator1.setId(1);
        testOperator1.setFullName("Operator 1");

        testOperator2 = new User();
        testOperator2.setId(2);
        testOperator2.setFullName("Operator 2");

        // Используем оригинальную коллекцию из сервиса
        openStatuses = LeastBusyAssignmentService.OPEN_CHAT_STATUSES;
    }

    // --- Тесты для findLeastBusyOperator ---

    @Test
    void findLeastBusyOperator_WhenOperatorsExist_ShouldReturnFirstOperatorFromList() {
        // Arrange
        // Мокируем метод репозитория с двумя аргументами, возвращающий список
        when(userRepository.findLeastBusyUser(eq(COMPANY_ID), eq(openStatuses)))
                .thenReturn(List.of(testOperator1, testOperator2)); // Возвращаем список

        // Act
        Optional<User> result = assignmentService.findLeastBusyOperator(COMPANY_ID);

        // Assert
        assertTrue(result.isPresent());
        // Сервис должен вернуть ПЕРВОГО из списка
        assertEquals(testOperator1, result.get());
        // Проверяем вызов метода репозитория с двумя аргументами
        verify(userRepository).findLeastBusyUser(eq(COMPANY_ID), eq(openStatuses));
    }

    @Test
    void findLeastBusyOperator_WhenNoOperators_ShouldReturnEmpty() {
        // Arrange
        // Мокируем метод репозитория с двумя аргументами, возвращающий пустой список
        when(userRepository.findLeastBusyUser(eq(COMPANY_ID), eq(openStatuses)))
                .thenReturn(Collections.emptyList());

        // Act
        Optional<User> result = assignmentService.findLeastBusyOperator(COMPANY_ID);

        // Assert
        assertTrue(result.isEmpty());
        verify(userRepository).findLeastBusyUser(eq(COMPANY_ID), eq(openStatuses));
    }

    @Test
        // ДОБАВЛЕНО: Тест на ошибку репозитория
    void findLeastBusyOperator_RepositoryThrowsException_ShouldThrowException() {
        // Arrange
        DataAccessException dbException = new DataAccessException("DB Error") {};
        when(userRepository.findLeastBusyUser(eq(COMPANY_ID), eq(openStatuses)))
                .thenThrow(dbException);

        // Act & Assert
        DataAccessException thrown = assertThrows(DataAccessException.class, () -> {
            assignmentService.findLeastBusyOperator(COMPANY_ID);
        });
        assertEquals(dbException, thrown); // Ожидаем проброс исходного исключения
        verify(userRepository).findLeastBusyUser(eq(COMPANY_ID), eq(openStatuses));
    }


    // --- Тесты для assignOperator ---

    @Test
    void assignOperator_WhenChatIsNull_ShouldReturnEmpty() {
        Optional<User> result = assignmentService.assignOperator(null);
        assertTrue(result.isEmpty());
        verifyNoInteractions(userRepository);
    }

    @Test
    void assignOperator_WhenCompanyIsNull_ShouldReturnEmpty() {
        Chat chat = new Chat();
        chat.setCompany(null); // Компания null
        Optional<User> result = assignmentService.assignOperator(chat);
        assertTrue(result.isEmpty());
        verifyNoInteractions(userRepository);
    }

    @Test
    void assignOperator_WithValidChat_ShouldReturnLeastBusyOperator() {
        // Arrange
        Chat chat = new Chat();
        Company company = new Company();
        company.setId(COMPANY_ID);
        chat.setCompany(company);

        // Мокируем репозиторий для вызова из findLeastBusyOperator
        when(userRepository.findLeastBusyUser(eq(COMPANY_ID), eq(openStatuses)))
                .thenReturn(List.of(testOperator2)); // Возвращаем одного оператора

        // Act
        Optional<User> result = assignmentService.assignOperator(chat);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testOperator2, result.get());
        verify(userRepository).findLeastBusyUser(eq(COMPANY_ID), eq(openStatuses));
    }

    @Test
        // ДОБАВЛЕНО: Тест на ошибку репозитория при вызове из assignOperator
    void assignOperator_RepositoryThrowsException_ShouldThrowException() {
        // Arrange
        Chat chat = new Chat();
        Company company = new Company();
        company.setId(COMPANY_ID);
        chat.setCompany(company);
        DataAccessException dbException = new DataAccessException("DB Error") {};
        when(userRepository.findLeastBusyUser(eq(COMPANY_ID), eq(openStatuses)))
                .thenThrow(dbException);

        // Act & Assert
        DataAccessException thrown = assertThrows(DataAccessException.class, () -> {
            assignmentService.assignOperator(chat);
        });
        assertEquals(dbException, thrown);
        verify(userRepository).findLeastBusyUser(eq(COMPANY_ID), eq(openStatuses));
    }

    // --- Тест для константы OPEN_CHAT_STATUSES ---
    @Test
    void openChatStatuses_ShouldContainCorrectStatuses() {
        // Act & Assert
        Collection<ChatStatus> statuses = LeastBusyAssignmentService.OPEN_CHAT_STATUSES;
        assertNotNull(statuses);
        assertEquals(5, statuses.size());
        assertTrue(statuses.contains(ChatStatus.ASSIGNED));
        assertTrue(statuses.contains(ChatStatus.IN_PROGRESS));
        assertTrue(statuses.contains(ChatStatus.PENDING_OPERATOR));
        assertTrue(statuses.contains(ChatStatus.PENDING_AUTO_RESPONDER));
        assertTrue(statuses.contains(ChatStatus.NEW));
        // Проверим, что нет лишних
        assertFalse(statuses.contains(ChatStatus.CLOSED));
        assertFalse(statuses.contains(ChatStatus.ARCHIVED));
    }
}
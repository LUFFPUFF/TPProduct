package com.example.domain.api.chat_service_api.service.impl;

import com.example.database.model.chats_messages_module.Notification;
import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.chats_messages_module.NotificationRepository;
import com.example.database.repository.company_subscription_module.UserRepository; // UserRepository не используется напрямую сервисом, можно убрать
import com.example.domain.api.chat_service_api.mapper.NotificationMapper;
import com.example.domain.api.chat_service_api.model.dto.notification.NotificationDTO;
import com.example.domain.api.chat_service_api.service.WebSocketMessagingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
// Добавляем капторы
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataAccessException; // Для имитации ошибки репозитория
import org.springframework.messaging.MessagingException; // Для имитации ошибки WebSocket

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt; // Используем anyInt для ID
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // Для удобства с BeforeEach
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationMapper notificationMapper;
    // UserRepository не нужен как зависимость сервиса NotificationServiceImpl
    // @Mock private UserRepository userRepository;
    @Mock private WebSocketMessagingService messagingService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    // Капторы
    @Captor private ArgumentCaptor<Notification> notificationCaptor;
    @Captor private ArgumentCaptor<String> userIdDestinationCaptor;
    @Captor private ArgumentCaptor<String> topicDestinationCaptor;
    @Captor private ArgumentCaptor<NotificationDTO> payloadCaptor;


    private User testUser;
    private Chat testChat;
    private Notification testNotification;
    private NotificationDTO testNotificationDto;
    private final Integer USER_ID = 1;
    private final Integer CHAT_ID = 10;
    private final Integer NOTIFICATION_ID = 100;
    private final String NOTIFICATION_TYPE = "CHAT_ASSIGNED";
    private final String NOTIFICATION_MESSAGE = "Chat assigned to you";
    private final String WEBSOCKET_DESTINATION = "/queue/notifications";


    @BeforeEach
    void setUp() {
        reset(notificationRepository, notificationMapper, messagingService); // Сброс моков

        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setEmail("user@example.com");

        testChat = new Chat();
        testChat.setId(CHAT_ID);

        testNotification = new Notification();
        testNotification.setId(NOTIFICATION_ID);
        testNotification.setUser(testUser);
        testNotification.setChat(testChat);
        testNotification.setType(NOTIFICATION_TYPE);
        testNotification.setMessage(NOTIFICATION_MESSAGE);
        testNotification.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        testNotification.setRead(false);

        testNotificationDto = new NotificationDTO();
        testNotificationDto.setId(NOTIFICATION_ID);
        testNotificationDto.setType(NOTIFICATION_TYPE);
        testNotificationDto.setMessage(NOTIFICATION_MESSAGE);
        testNotificationDto.setRead(false);
        // Не мокируем createdAt, пусть приходит из DTO
    }

    // --- Тесты для createNotification ---

    @Test
    void createNotification_ShouldSaveMapAndSendNotification() {
        // Arrange
        // Мокируем save, чтобы вернуть сущность с присвоенным ID
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(NOTIFICATION_ID); // Имитируем присвоение ID базой данных
            return saved;
        });
        // Мокируем маппер
        when(notificationMapper.toDto(any(Notification.class))).thenReturn(testNotificationDto);
        // Мокируем WebSocket сервис
        doNothing().when(messagingService).sendToUser(anyString(), anyString(), any(NotificationDTO.class));

        // Act
        NotificationDTO result = notificationService.createNotification(
                testUser, testChat, NOTIFICATION_TYPE, NOTIFICATION_MESSAGE);

        // Assert
        assertNotNull(result);
        assertEquals(testNotificationDto, result); // Проверяем возвращенный DTO

        // Проверяем сохранение
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification savedEntity = notificationCaptor.getValue();
        assertEquals(testUser, savedEntity.getUser());
        assertEquals(testChat, savedEntity.getChat());
        assertEquals(NOTIFICATION_TYPE, savedEntity.getType());
        assertEquals(NOTIFICATION_MESSAGE, savedEntity.getMessage());
        assertNotNull(savedEntity.getCreatedAt());
        assertFalse(savedEntity.isRead());

        // Проверяем маппинг (проверяем, что был вызван с правильным объектом)
        verify(notificationMapper).toDto(savedEntity); // Проверяем маппинг сохраненной сущности

        // Проверяем отправку WebSocket
        verify(messagingService).sendToUser(userIdDestinationCaptor.capture(), topicDestinationCaptor.capture(), payloadCaptor.capture());
        assertEquals(USER_ID.toString(), userIdDestinationCaptor.getValue());
        assertEquals(WEBSOCKET_DESTINATION, topicDestinationCaptor.getValue());
        assertEquals(testNotificationDto, payloadCaptor.getValue());
    }

    @Test
    void createNotification_NullUser_ShouldThrowNullPointerException_AfterSaveAndMap() {
        // Arrange
        // Мокируем save и map, чтобы они не мешали дойти до NPE
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification); // Возвращаем фиктивный объект
        when(notificationMapper.toDto(any(Notification.class))).thenReturn(testNotificationDto); // Возвращаем фиктивный DTO

        // Act & Assert
        // Ожидаем NullPointerException при вызове getId() на null user
        assertThrows(NullPointerException.class, () -> {
            notificationService.createNotification(null, testChat, NOTIFICATION_TYPE, NOTIFICATION_MESSAGE);
        });

        // ИСПРАВЛЕНО: Проверяем, что взаимодействия БЫЛИ перед NPE
        verify(notificationRepository).save(notificationCaptor.capture());
        assertNull(notificationCaptor.getValue().getUser()); // Убедимся, что null user был передан в save

        verify(notificationMapper).toDto(any(Notification.class));

        // Проверяем, что отправка по WebSocket не вызывалась (т.к. упало раньше)
        verify(messagingService, never()).sendToUser(anyString(), anyString(), any());
    }

    @Test
    void createNotification_NullChat_ShouldSaveAndSendWithNullChat() {
        // Сервис позволяет null chat, проверим это
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(NOTIFICATION_ID);
            return saved;
        });
        testNotificationDto.setChat(null); // Ожидаем null chat в DTO
        when(notificationMapper.toDto(any(Notification.class))).thenReturn(testNotificationDto);

        NotificationDTO result = notificationService.createNotification(
                testUser, null, NOTIFICATION_TYPE, NOTIFICATION_MESSAGE);

        assertNotNull(result);
        verify(notificationRepository).save(notificationCaptor.capture());
        assertNull(notificationCaptor.getValue().getChat()); // Проверяем, что чат null
        verify(notificationMapper).toDto(any(Notification.class));
        verify(messagingService).sendToUser(anyString(), anyString(), eq(testNotificationDto));
    }

    @Test
    void createNotification_RepositorySaveFails_ShouldThrowException() {
        // Arrange
        DataAccessException dbException = new DataAccessException("Save failed") {};
        when(notificationRepository.save(any(Notification.class))).thenThrow(dbException);

        // Act & Assert
        DataAccessException thrown = assertThrows(DataAccessException.class, () -> {
            notificationService.createNotification(testUser, testChat, NOTIFICATION_TYPE, NOTIFICATION_MESSAGE);
        });
        assertEquals(dbException, thrown);
        verify(notificationRepository).save(any(Notification.class));
        verifyNoInteractions(notificationMapper, messagingService); // Не должно дойти до маппинга и отправки
    }

    @Test
    void createNotification_MapperFails_ShouldThrowException() {
        // Arrange
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        RuntimeException mapperException = new RuntimeException("Mapping failed");
        when(notificationMapper.toDto(testNotification)).thenThrow(mapperException);

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            notificationService.createNotification(testUser, testChat, NOTIFICATION_TYPE, NOTIFICATION_MESSAGE);
        });
        assertEquals(mapperException, thrown);
        verify(notificationRepository).save(any(Notification.class));
        verify(notificationMapper).toDto(testNotification);
        verifyNoInteractions(messagingService); // Не должно дойти до отправки
    }

    @Test
    void createNotification_WebSocketSendFails_ShouldThrowException() {
        // Arrange
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        when(notificationMapper.toDto(testNotification)).thenReturn(testNotificationDto);
        MessagingException wsException = new MessagingException("Send failed");
        // Мокируем ошибку отправки
        doThrow(wsException).when(messagingService).sendToUser(anyString(), anyString(), any(NotificationDTO.class));

        // Act & Assert
        MessagingException thrown = assertThrows(MessagingException.class, () -> {
            notificationService.createNotification(testUser, testChat, NOTIFICATION_TYPE, NOTIFICATION_MESSAGE);
        });
        assertEquals(wsException, thrown); // Сервис пробрасывает исключение
        verify(notificationRepository).save(any(Notification.class));
        verify(notificationMapper).toDto(testNotification);
        verify(messagingService).sendToUser(eq(USER_ID.toString()), eq(WEBSOCKET_DESTINATION), eq(testNotificationDto));
    }

    // --- Тесты для getUnreadNotifications ---

    @Test
    void getUnreadNotifications_WhenFound_ShouldReturnDtoList() {
        // Arrange
        Notification readNotification = new Notification(); // Не должен вернуться
        readNotification.setRead(true);
        List<Notification> notifications = List.of(testNotification); // Только непрочитанное
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(USER_ID))
                .thenReturn(notifications);
        when(notificationMapper.toDto(testNotification)).thenReturn(testNotificationDto);

        // Act
        List<NotificationDTO> result = notificationService.getUnreadNotifications(USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testNotificationDto, result.get(0));
        verify(notificationRepository).findByUserIdAndIsReadFalseOrderByCreatedAtDesc(USER_ID);
        verify(notificationMapper).toDto(testNotification);
    }

    @Test
    void getUnreadNotifications_WhenNotFound_ShouldReturnEmptyList() {
        // Arrange
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(USER_ID))
                .thenReturn(Collections.emptyList());

        // Act
        List<NotificationDTO> result = notificationService.getUnreadNotifications(USER_ID);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(notificationRepository).findByUserIdAndIsReadFalseOrderByCreatedAtDesc(USER_ID);
        verifyNoInteractions(notificationMapper);
    }

    @Test
    void getUnreadNotifications_RepositoryFails_ShouldThrowException() {
        // Arrange
        DataAccessException dbException = new DataAccessException("Find failed") {};
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(USER_ID)).thenThrow(dbException);

        // Act & Assert
        DataAccessException thrown = assertThrows(DataAccessException.class, () -> {
            notificationService.getUnreadNotifications(USER_ID);
        });
        assertEquals(dbException, thrown);
        verify(notificationRepository).findByUserIdAndIsReadFalseOrderByCreatedAtDesc(USER_ID);
        verifyNoInteractions(notificationMapper);
    }

    // --- Тесты для getAllNotifications (аналогично getUnread) ---

    @Test
    void getAllNotifications_WhenFound_ShouldReturnDtoList() {
        // Arrange
        Notification readNotification = new Notification(); // Должен вернуться
        readNotification.setRead(true);
        readNotification.setId(NOTIFICATION_ID + 1);
        NotificationDTO readDto = new NotificationDTO();
        readDto.setRead(true);
        readDto.setId(NOTIFICATION_ID + 1);

        List<Notification> notifications = List.of(testNotification, readNotification);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(notifications);
        when(notificationMapper.toDto(testNotification)).thenReturn(testNotificationDto);
        when(notificationMapper.toDto(readNotification)).thenReturn(readDto);

        // Act
        List<NotificationDTO> result = notificationService.getAllNotifications(USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(testNotificationDto));
        assertTrue(result.contains(readDto));
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(USER_ID);
        verify(notificationMapper, times(2)).toDto(any(Notification.class));
    }

    @Test
    void getAllNotifications_WhenNotFound_ShouldReturnEmptyList() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(Collections.emptyList());
        List<NotificationDTO> result = notificationService.getAllNotifications(USER_ID);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(USER_ID);
        verifyNoInteractions(notificationMapper);
    }

    // --- Тесты для markNotificationAsRead ---

    @Test
    void markNotificationAsRead_WhenUnreadExists_ShouldUpdateSaveSendAndReturnTrue() {
        // Arrange
        testNotification.setRead(false); // Убедимся, что не прочитано
        NotificationDTO updatedDto = new NotificationDTO(); // DTO после обновления
        updatedDto.setId(NOTIFICATION_ID);
        updatedDto.setRead(true); // Главное изменение
        updatedDto.setType(NOTIFICATION_TYPE);
        updatedDto.setMessage(NOTIFICATION_MESSAGE);

        when(notificationRepository.findByIdAndUserId(NOTIFICATION_ID, USER_ID)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0)); // Возвращаем обновленную
        when(notificationMapper.toDto(any(Notification.class))).thenReturn(updatedDto); // Маппер возвращает обновленный DTO

        // Act
        boolean result = notificationService.markNotificationAsRead(NOTIFICATION_ID, USER_ID);

        // Assert
        assertTrue(result);
        // Проверяем сущность перед сохранением
        verify(notificationRepository).save(notificationCaptor.capture());
        assertTrue(notificationCaptor.getValue().isRead()); // Флаг должен быть true
        // Проверяем маппинг
        verify(notificationMapper).toDto(notificationCaptor.getValue());
        // Проверяем отправку по WebSocket
        verify(messagingService).sendToUser(userIdDestinationCaptor.capture(), topicDestinationCaptor.capture(), payloadCaptor.capture());
        assertEquals(USER_ID.toString(), userIdDestinationCaptor.getValue());
        assertEquals(WEBSOCKET_DESTINATION, topicDestinationCaptor.getValue());
        assertEquals(updatedDto, payloadCaptor.getValue()); // Отправляем обновленный DTO
    }

    @Test
    void markNotificationAsRead_WhenAlreadyRead_ShouldDoNothingAndReturnTrue() {
        // Arrange
        testNotification.setRead(true); // Уже прочитано
        when(notificationRepository.findByIdAndUserId(NOTIFICATION_ID, USER_ID)).thenReturn(Optional.of(testNotification));

        // Act
        boolean result = notificationService.markNotificationAsRead(NOTIFICATION_ID, USER_ID);

        // Assert
        assertTrue(result);
        verify(notificationRepository).findByIdAndUserId(NOTIFICATION_ID, USER_ID);
        // Ничего больше не должно вызываться
        verify(notificationRepository, never()).save(any());
        verify(notificationMapper, never()).toDto(any());
        verify(messagingService, never()).sendToUser(anyString(), anyString(), any());
    }

    @Test
    void markNotificationAsRead_WhenNotFound_ShouldReturnFalse() {
        // Arrange
        when(notificationRepository.findByIdAndUserId(NOTIFICATION_ID, USER_ID)).thenReturn(Optional.empty());

        // Act
        boolean result = notificationService.markNotificationAsRead(NOTIFICATION_ID, USER_ID);

        // Assert
        assertFalse(result);
        verify(notificationRepository).findByIdAndUserId(NOTIFICATION_ID, USER_ID);
        verify(notificationRepository, never()).save(any());
        verify(notificationMapper, never()).toDto(any());
        verify(messagingService, never()).sendToUser(anyString(), anyString(), any());
    }

    @Test
    void markNotificationAsRead_RepositorySaveFails_ShouldThrowException() {
        // Arrange
        testNotification.setRead(false);
        when(notificationRepository.findByIdAndUserId(NOTIFICATION_ID, USER_ID)).thenReturn(Optional.of(testNotification));
        DataAccessException dbException = new DataAccessException("Save failed") {};
        when(notificationRepository.save(any(Notification.class))).thenThrow(dbException);

        // Act & Assert
        DataAccessException thrown = assertThrows(DataAccessException.class, () -> {
            notificationService.markNotificationAsRead(NOTIFICATION_ID, USER_ID);
        });
        assertEquals(dbException, thrown);
        verify(notificationRepository).findByIdAndUserId(NOTIFICATION_ID, USER_ID);
        verify(notificationRepository).save(any(Notification.class));
        verifyNoInteractions(notificationMapper, messagingService); // Не должно дойти до маппинга/отправки
    }

    @Test
    void markNotificationAsRead_MapperFails_ShouldThrowException() {
        // Arrange
        testNotification.setRead(false);
        when(notificationRepository.findByIdAndUserId(NOTIFICATION_ID, USER_ID)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification); // Сохранение успешно
        RuntimeException mapperException = new RuntimeException("Mapping failed");
        when(notificationMapper.toDto(testNotification)).thenThrow(mapperException); // Ошибка маппинга

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            notificationService.markNotificationAsRead(NOTIFICATION_ID, USER_ID);
        });
        assertEquals(mapperException, thrown);
        verify(notificationRepository).save(any(Notification.class));
        verify(notificationMapper).toDto(testNotification);
        verifyNoInteractions(messagingService); // Не дошло до отправки
    }

    @Test
    void markNotificationAsRead_WebSocketSendFails_ShouldThrowException() {
        // Arrange
        testNotification.setRead(false);
        NotificationDTO updatedDto = new NotificationDTO();
        updatedDto.setRead(true);
        when(notificationRepository.findByIdAndUserId(NOTIFICATION_ID, USER_ID)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        when(notificationMapper.toDto(testNotification)).thenReturn(updatedDto);
        MessagingException wsException = new MessagingException("Send failed");
        doThrow(wsException).when(messagingService).sendToUser(anyString(), anyString(), any(NotificationDTO.class));

        // Act & Assert
        MessagingException thrown = assertThrows(MessagingException.class, () -> {
            notificationService.markNotificationAsRead(NOTIFICATION_ID, USER_ID);
        });
        assertEquals(wsException, thrown); // Сервис пробрасывает исключение
        verify(messagingService).sendToUser(eq(USER_ID.toString()), eq(WEBSOCKET_DESTINATION), eq(updatedDto));
    }

    // --- Тест для sendNotificationToUser ---

    @Test
    void sendNotificationToUser_ShouldThrowUnsupportedOperationException() {
        // Arrange
        NotificationDTO dtoToSend = new NotificationDTO();

        // Act & Assert
        assertThrows(UnsupportedOperationException.class, () -> {
            notificationService.sendNotificationToUser(USER_ID, dtoToSend);
        });
    }
}
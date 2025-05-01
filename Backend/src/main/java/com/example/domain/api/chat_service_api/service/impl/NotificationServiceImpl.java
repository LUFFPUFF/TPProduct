package com.example.domain.api.chat_service_api.service.impl;

import com.example.database.model.chats_messages_module.Notification;
import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.chats_messages_module.NotificationRepository;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.chat_service_api.mapper.NotificationMapper;
import com.example.domain.api.chat_service_api.model.dto.notification.NotificationDTO;
import com.example.domain.api.chat_service_api.service.INotificationService;
import com.example.domain.api.chat_service_api.service.WebSocketMessagingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements INotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final UserRepository userRepository;
    private final WebSocketMessagingService messagingService;

    @Override
    @Transactional
    public NotificationDTO createNotification(User user, Chat chat, String type, String message) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setChat(chat);
        notification.setType(type);
        notification.setMessage(message);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);

        Notification savedNotification = notificationRepository.save(notification);

        NotificationDTO notificationDTO = notificationMapper.toDto(savedNotification);

        messagingService.sendToUser(user.getId().toString(), "/queue/notifications", notificationDTO);

        return notificationDTO;
    }

    @Override
    public List<NotificationDTO> getUnreadNotifications(Integer userId) {
        // TODO: Проверка прав! Должен ли текущий пользователь иметь доступ к уведомлениям пользователя с userId?

        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        return notifications.stream()
                .map(notificationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationDTO> getAllNotifications(Integer userId) {
        // TODO: Проверка прав! Аналогично getUnreadNotifications.

        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return notifications.stream()
                .map(notificationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public boolean markNotificationAsRead(Integer notificationId, Integer userId) {
        // TODO: Проверка прав уже должна встроена в findByIdAndUserId - уведомление должно принадлежать пользователю userId.

        Optional<Notification> notificationOptional = notificationRepository.findByIdAndUserId(notificationId, userId);
        if (notificationOptional.isEmpty()) {
            return false;
        }

        Notification notification = notificationOptional.get();
        if (notification.isRead()) {
            return true;
        }

        notification.setRead(true);

        Notification updatedNotification = notificationRepository.save(notification);
        NotificationDTO updatedNotificationDTO = notificationMapper.toDto(updatedNotification);
        messagingService.sendToUser(userId.toString(), "/queue/notifications", updatedNotificationDTO);

        return true;
    }

    @Override
    public void sendNotificationToUser(Integer userId, NotificationDTO notification) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

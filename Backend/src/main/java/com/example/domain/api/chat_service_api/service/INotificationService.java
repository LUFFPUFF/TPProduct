package com.example.domain.api.chat_service_api.service;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.domain.api.chat_service_api.model.dto.notification.NotificationDTO;

import java.util.List;

public interface INotificationService {

    /**
     * Создает и сохраняет новое уведомление для пользователя.
     * @param user Пользователь, которому предназначено уведомление.
     * @param chat Чат, к которому относится уведомление (опционально).
     * @param type Тип уведомления (строка).
     * @param message Текст уведомления.
     * @return Созданный NotificationDTO.
     */
    NotificationDTO createNotification(User user, Chat chat, String type, String message);

    /**
     * Получает список непрочитанных уведомлений для пользователя.
     * @param userId ID пользователя.
     * @return Список NotificationDTO.
     */
    List<NotificationDTO> getUnreadNotifications(Integer userId);

    /**
     * Получает список всех уведомлений для пользователя.
     * @param userId ID пользователя.
     * @return Список NotificationDTO.
     */
    List<NotificationDTO> getAllNotifications(Integer userId);

    /**
     * Помечает уведомление как прочитанное.
     * @param notificationId ID уведомления.
     * @param userId ID пользователя, который пометил (для проверки прав).
     * @return True, если успешно обновлено, false иначе.
     */
    boolean markNotificationAsRead(Integer notificationId, Integer userId);

    void sendNotificationToUser(Integer userId, NotificationDTO notification);
}

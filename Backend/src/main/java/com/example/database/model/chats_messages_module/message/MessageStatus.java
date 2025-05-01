package com.example.database.model.chats_messages_module.message;

public enum MessageStatus {
    SENT,       // Отправлено (из системы во внешний канал или по WebSocket)
    DELIVERED,  // Доставлено во внешний канал/клиенту
    READ,       // Прочитано клиентом во внешнем канале или оператором в UI
    FAILED      // Не удалось отправить
}

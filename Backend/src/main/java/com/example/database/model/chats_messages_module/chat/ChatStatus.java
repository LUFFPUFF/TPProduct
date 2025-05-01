package com.example.database.model.chats_messages_module.chat;

public enum ChatStatus {
    NEW,                    // Только создан, ждет обработки автоответчиком
    PENDING_AUTO_RESPONDER, // Обрабатывается автоответчиком
    PENDING_OPERATOR,       // Требует внимания оператора (после автоответчика или эскалации)
    ASSIGNED,               // Назначен конкретному оператору
    IN_PROGRESS,            // Оператор активно общается (можно использовать вместе с ASSIGNED)
    CLOSED,                 // Чат закрыт оператором
    ARCHIVED                // Чат архивирован
}

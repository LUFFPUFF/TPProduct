# 📦 Chat Module API – UI Endpoints Overview

Все эндпоинты пользовательского интерфейса для работы с чатами располагаются под общим префиксом:  
**`/api/ui/chats`**

---

## 🔍 Получение списка чатов

**GET `/api/ui/chats/my`**  
Возвращает список чатов, назначенных текущему оператору, а также чаты в ожидании назначения.

**Query-параметры:**
- `status` (опционально, многократно): Фильтрация по статусам. Возможные значения:
    - `PENDING_AUTO_RESPONDER`
    - `PENDING_OPERATOR`
    - `ASSIGNED`
    - `IN_PROGRESS`
    - `CLOSED`
    - `ARCHIVED`

**Пример запроса:** GET /api/ui/chats/my?status=ASSIGNED&status=PENDING_OPERATOR

---

## 🧾 Получение информации о конкретном чате

**GET `/api/ui/chats/{chatId}/details`**  
Возвращает подробную информацию о чате: история сообщений, данные клиента и оператора.

**Path-параметры:**
- `chatId` (Integer): ID нужного чата

**Пример:** GET /api/ui/chats/123/details

---

## 💬 Отправка сообщения

**POST `/api/ui/chats/messages`**  
Отправляет новое сообщение от оператора в указанный чат.

**Request Body (JSON):**
```json
{
  "chatId": 123,
  "content": "Текст сообщения"
}
```

---

## ❌ Закрытие чата

**POST `/api/ui/chats/{chatId}/close`**

Помечает чат как закрытый.

**Path-параметры:**
- `chatId` (Integer): ID нужного чата

---

## ✅ Пометка сообщений как прочитанных

**POST `/api/ui/chats/{chatId}/messages/read`**

Обновляет статус сообщений на "прочитано".

**Path-параметры:**
- `chatId` (Integer): ID нужного чата

**Request Body (JSON):**
```json
{
  "messageIds": [123, 124, 125]
}
```

---

## 🧪 Создание тестового чата

**POST `/api/ui/chats/create-test-chat`**

Создаёт тестовый чат для текущего оператора. Используется в демонстрационных и тестовых целях.

---

## 🔔 Получение уведомлений

**GET `/api/ui/notifications/my`**

Возвращает список уведомлений для текущего оператора.

**Path-параметры:**
- `unreadOnly ` (boolean, по умолчанию false): Если true — только непрочитанные уведомления.

**Пример:** GET /api/ui/notifications/my?unreadOnly=true
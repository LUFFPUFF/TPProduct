# API Документация для Чатов

Это руководство предназначено для фронтенд-разработчиков и описывает взаимодействие с бэкенд-системой чатов через WebSocket и REST API.

## Часть 1: WebSocket API (STOMP через WebSocket)

Взаимодействие с сервером в реальном времени осуществляется через WebSocket с использованием протокола STOMP.

### 1.1. Подключение и Аутентификация

- **Эндпоинт для подключения:** `/ws` (настраивается свойством `websocket.endpoint`)
- **Аутентификация:** При установлении STOMP-соединения (в кадре `CONNECT`) необходимо передать валидный JWT Access Token в заголовке:
  - **Имя заголовка:** `Authorization` (настраивается свойством `websocket.security.jwt.header-name`)
  - **Значение:** `Bearer <ваш_jwt_токен>` (префикс настраивается свойством `websocket.security.jwt.token-prefix`)

**Пример подключения чатик дал (JavaScript, библиотека `@stomp/stompjs`):**
```javascript
import { Client } from '@stomp/stompjs';

const client = new Client({
  brokerURL: 'ws://your-server-address/ws', // Замените на ваш реальный URL сервера
  connectHeaders: {
    Authorization: `Bearer ${your_jwt_token}`, // Переменная, содержащая JWT токен
  },
  debug: (str) => {
    console.log('STOMP: ' + str); // Логирование STOMP сообщений для отладки
  },
  reconnectDelay: 5000, // Задержка перед попыткой переподключения (в мс)
  heartbeatIncoming: 4000, // Ожидание ping от сервера (в мс)
  heartbeatOutgoing: 4000, // Отправка pong серверу (в мс)
});

client.onConnect = (frame) => {
  console.log('Успешно подключено: ' + frame);
  // Здесь происходит подписка на необходимые топики
  // Например: client.subscribe('/topic/chat/123/messages', message => { ... });
};

client.onStompError = (frame) => {
  console.error('Ошибка STOMP брокера: ' + frame.headers['message']);
  console.error('Дополнительные детали: ' + frame.body);
};

client.activate(); // Активация клиента для подключения
```

### 1.2. Топики для подписки (Сообщения от Сервера к Клиенту)

Клиент должен подписаться на эти топики, чтобы получать обновления от сервера в реальном времени.

#### 1.2.1. Обновления по конкретному чату

### Обновление статуса чата
**Топик** `/topic/chat/{chatId}/status` (где {chatId} - ID конкретного чата)
- **Payload (Тело сообщения):** Объект ChatDTO (сервер отправляет более полную информацию, UI может мапить ее в UIChatDto или использовать напрямую).
  - Это событие информирует об изменениях статуса чата (например, PENDING_OPERATOR -> ASSIGNED), назначении оператора, обновлении времени последнего сообщения и т.д.
- Пример ChatDTO (как он будет отправлен сервером):
```json
{
  "id": 123,
  "client": { 
    "id": 10,
    "name": "Иван Иванов",
    "tag": "VIP", 
    "typeClient": "REGULAR" 
  },
  "operator": { //может быть null
    "id": 5,
    "fullName": "Анна Петрова",
    "profilePicture": "https://example.com/avatars/operator5.jpg", 
    "status": "ONLINE"
  },
  "chatChannel": "VK", 
  "status": "ASSIGNED",
  "createdAt": "2023-10-27T10:00:00Z",
  "lastMessageAt": "2023-10-27T10:15:00Z",
  "lastMessageSnippet": "Здравствуйте! Чем могу помочь?",
  "unreadMessagesCount": 0
}
```

### Новые сообщения в чате / Обновления статуса существующих сообщений
**Топик** `/topic/chat/{chatId}/messages` (где {chatId} - ID конкретного чата)
- **Payload (Тело сообщения):** Объект UiMessageDto
  - Используется для доставки новых сообщений и для уведомлений об изменении статуса уже существующих сообщений.
- Пример UiMessageDto:
```json
{
  "id": 789,                   
  "chatId": 123,                
  "senderType": "OPERATOR",       
  "senderName": "Анна Петрова", 
  "content": "Ваш заказ #12345 был успешно подтвержден.",
  "sentAt": "2023-10-27T10:15:00Z",
  "status": "SENT"
}
```

### Событие "печатает" - пока не обработано в коде
**Топик** `/topic/chat/{chatId}/typing` (где {chatId} - ID конкретного чата)
- **Payload (Тело сообщения):** Объект, описывающий, кто печатает.
- Пример:
```json
нет примера
```

#### 1.2.2. Общие топики для компании (для операторов/менеджеров)

### Новые чаты в очереди ожидания компании

- **Топик** `/topic/company/{companyId}/chats/pending` 
- **Payload (Тело сообщения):** ChatDTO.

### Чаты, назначенные в компании (общая лента)

- **Топик** `/topic/company/{companyId}/chats/assigne`
- **Payload (Тело сообщения):** ChatDTO.

#### 1.2.3. Персональные топики пользователя

Подписка на адреса вида /user/queue/...

### Общие персональные уведомления

- **Топик** `/user/queue/notifications`
- **Payload (Тело сообщения):** UiNotificationDto.
- Пример UiNotificationDto
```json
{
  "id": 1,
  "chatId": 2,
  "type": "INFO",
  "message": "Вам был назначен новый чат #124 от клиента 'Петр Сидоров'.",
  "createdAt": "2023-10-27T11:00:00Z",
  "isRead": false
}
```

### Уведомление о назначении чата оператору

- **Топик** `/user/queue/assigned-chats`
- **Payload (Тело сообщения):** ChatDTO.

### Уведомление о закрытии чата оператора

- **Топик** `/user/queue/closed-chats`
- **Payload (Тело сообщения):** ChatDTO.

### 1.3. Сообщения от Клиента к Серверу через WebSocket

Отправка на адреса, начинающиеся с /app

### Уведомление о назначении чата оператору

- **Топик** `/app/chat/{chatId}/send_message`
- **Payload (Тело сообщения):** Пример.
```json
{
  "content": "Текст нового сообщения."
}
```
**Ответ сервера:** Новое UiMessageDto придет по топику `/topic/chat/{chatId}/messages`.

### Отправка события "печатает"

- **Топик** `/app/chat/{chatId}/typing`
- **Payload (Тело сообщения):** Пример.
```json
{
  "isTyping": true
}
```

### Отметка сообщений как прочитанных

- **Топик** `/app/chat/{chatId}/messages_read`
- **Payload (Тело сообщения):** MarkUIMessagesAsReadRequestUI.
```json
{
  "messageIds": [101, 102]
}
```
**Ответ сервера:** Новое UiMessageDto придет по топику `/topic/chat/{chatId}/messages`.

---

# Часть 2: REST API (ChatUiController)

Базовый URL для всех эндпоинтов: /api/ui/chats

**GET `/{chatId}/details`**  
- **Описание:** Получает полные детали чата
- **Параметры пути:** chatId (integer)

**Response Body (JSON):**
```json
{
  "id": 123,
  "status": "NEW",
  "client": { 
    "id": 10,
    "name": "Иван Иванов",
    "tag": "VIP",
    "typeClient": "REGULAR"
  },
  "assignedOperator": {  //может быть null
    "id": 5,
    "fullName": "Анна Петрова",
    "profilePicture": "https://example.com/avatars/operator5.jpg",
    "status": "ONLINE"
  },
  "channel": "VK",
  "createdAt": "2023-10-27T11:00:00Z",
  "assignedAt": "2023-10-27T11:00:00Z",
  "closedAt": "2023-10-27T11:00:00Z",
  "messages": [ 
    {
      "id": 788,
      "chatId": 123,
      "senderType": "CLIENT",
      "senderName": "Иван Иванов",
      "content": "Привет!",
      "sentAt": "2023-10-27T10:02:00Z",
      "status": "READ"
    }
  ]
}
```

**GET `/my`**
- **Описание:** Получает список чатов текущего пользователя.
- **Параметры запрос:** status (string, опциональный, многозначный).

**Response Body (JSON):**
```json
[
  {
    "id": 123,
    "clientName": "Иван Иванов",
    "source": "VK", 
    "lastMessageContent": "Здравствуйте!",
    "lastMessageAt": "2023-10-27T10:15:00Z",
    "unreadCount": 0,
    "status": "ASSIGNED", 
    "assignedOperatorName": "Анна Петрова"
  }
]
```

**GET `/operator/{operatorId}`**
- **Описание:** Получает список чатов указанного оператора.
- **Параметры пути:** operatorId (integer)
- **Параметры запрос:** status (string, опциональный, многозначный).

**Response Body (JSON):**
```json
[
  {
    "id": 123,
    "clientName": "Иван Иванов",
    "source": "VK", 
    "lastMessageContent": "Здравствуйте!",
    "lastMessageAt": "2023-10-27T10:15:00Z",
    "unreadCount": 0,
    "status": "ASSIGNED", 
    "assignedOperatorName": "Анна Петрова"
  }
]
```

**GET `/client/{clientId}`**
- **Описание:** Получает список чатов указанного клиента.
- **Параметры пути:** operatorId (integer)
- **Параметры запрос:** status (string, опциональный, многозначный).

**Response Body (JSON):**
```json
[
  {
    "id": 123,
    "clientName": "Иван Иванов",
    "source": "VK", 
    "lastMessageContent": "Здравствуйте!",
    "lastMessageAt": "2023-10-27T10:15:00Z",
    "unreadCount": 0,
    "status": "ASSIGNED", 
    "assignedOperatorName": "Анна Петрова"
  }
]
```

**POST `/create-test-chat`**
- **Описание:** Создает тестовый чат для текущего пользователя.

**Response Body (JSON):**
```json
{
  "id": 123,
  "status": "TEST",
  "client": {
    "id": 10,
    "name": "Иван Иванов",
    "tag": "VIP",
    "typeClient": "ONLINE"
  },
  "assignedOperator": {  
    "id": 5,
    "fullName": "Иван Иванов",
    "profilePicture": "https://example.com/avatars/operator5.jpg",
    "status": "ONLINE"
  },
  "channel": "TEST",
  "createdAt": "2023-10-27T11:00:00Z",
  "assignedAt": "2023-10-27T11:00:00Z",
  "closedAt": "2023-10-27T11:00:00Z",
  "messages": [
    {
      "id": 788,
      "chatId": 123,
      "senderType": "CLIENT",
      "senderName": "Иван Иванов",
      "content": "Привет!",
      "sentAt": "2023-10-27T10:02:00Z",
      "status": "READ"
    }
  ]
}
```

**POST `/messages`**
- **Описание:** Отправка сообщения
- **Тело запроса (UiSendUiMessageRequest):**
```json
{
  "chatId": 123,
  "content": "Привет!"
}
```

**Response Body (JSON):**
```json
{
  "id": 1,
  "chatId": 123,
  "senderType": "CLIENT",
  "senderName": "Иван Иванов",
  "content": "Привет!",
  "sentAt": "2023-10-27T10:02:00Z",
  "status": "READ"
}
```

**POST `/`** - пока что не реализовано в UI
- **Описание:** Создание чата из UI оператором/менеджером.
- **Тело запроса (CreateChatRequestDTO):**
```json
{
  "clientId": 25,
  "companyId": 2,
  "chatChannel": "EMAIL",
  "initialMessageContent": "Первое сообщение."
}
```

**Response Body (JSON):**
```json
{
  "id": 123,
  "status": "TEST",
  "client": {
    "id": 10,
    "name": "Иван Иванов",
    "tag": "VIP",
    "typeClient": "ONLINE"
  },
  "assignedOperator": {
    "id": 5,
    "fullName": "Иван Иванов",
    "profilePicture": "https://example.com/avatars/operator5.jpg",
    "status": "ONLINE"
  },
  "channel": "TEST",
  "createdAt": "2023-10-27T11:00:00Z",
  "assignedAt": "2023-10-27T11:00:00Z",
  "closedAt": "2023-10-27T11:00:00Z",
  "messages": [
    {
      "id": 788,
      "chatId": 123,
      "senderType": "CLIENT",
      "senderName": "Иван Иванов",
      "content": "Привет!",
      "sentAt": "2023-10-27T10:02:00Z",
      "status": "READ"
    }
  ]
}
```

**GET `/notifications/my`** 
- **Описание:** Получает уведомления текущего пользователя.
- **Параметры запроса:** unreadOnly (boolean, опциональный, default: false)

**Response Body (JSON):**
```json
[
  {
    "id": 1,
    "chatId": 2,
    "type": "Новое уведомление",
    "message": "Какое то сообщение",
    "createdAt": "2023-10-27T11:00:00Z",
    "read": false
  }
]
```

**GET `/{chatId}/messages/read`**
- **Описание:** Отметка сообщений прочитанными 
- **Параметры запроса:** chatId
- **Тело запроса** (MarkUIMessagesAsReadRequestUI):

```json
{
  "messageIds": [101, 102]
}
```

**POST `/{chatId}/close`**
- **Описание:** Закрытие чата.
- **Параметры запроса:** chatId


**GET `/waiting`**
- **Описание:** Получает список чатов, ожидающих оператора.

**Response Body (JSON):**
```json
[
  {
    "id": 123,
    "clientName": "Иван Иванов",
    "source": "VK",
    "lastMessageContent": "Здравствуйте!",
    "lastMessageAt": "2023-10-27T10:15:00Z",
    "unreadCount": 0,
    "status": "ASSIGNED",
    "assignedOperatorName": "Анна Петрова"
  }
]
```

**GET `/assign`**
- **Описание:** Назначение оператора на чат.
- **Тело запроса** (AssignChatRequestDTO):

```json
{
  "chatId": 456,
  "operatorId": 7 
}
```

**Response Body (JSON):**
```json
Я устал их писать - ChatUIDetailsDTO
```

**POST `/{chatId}/link-operator/{operatorId}`**
- **Описание:** Ручная привязка оператора к чату.
- **Параметры запроса:** chatId, operatorId 

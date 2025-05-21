# API Документация для Управления Интеграциями

Это руководство предназначено для фронтенд-разработчиков и описывает взаимодействие с бэкенд-системой для управления конфигурациями интеграций через REST API.

## Общая информация

- **Базовый URL:** `/api/ui/integration`

---

## 1. Интеграция с Telegram

Эндпоинты для управления конфигурацией интеграции с Telegram.

### 1.1. Создать или обновить конфигурацию Telegram

- **Метод:** `POST`
- **Путь:** `/api/ui/integration/telegram`
- **Описание:** Создает новую конфигурацию Telegram для компании текущего пользователя или обновляет существующую.
- **Тело запроса (JSON):** `CreateTelegramConfigurationRequest`
```json
{
    "botUsername": "my_company_bot",
    "botToken": "123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11"
}
 ```

- **Тело ответа (JSON):** `IntegrationTelegramDto`
```json
{
  "id": 1,
  "companyId": 101,
  "botUsername": "my_company_bot",
  "createdAt": "2023-10-28T10:00:00Z",
  "updatedAt": "2023-10-28T10:05:00Z"
}
```

### 1.2. Получить все конфигурации Telegram для компании

- **Метод:** `GET`
- **Путь:** `/api/ui/integration/telegram`
- **Описание:** Возвращает список (обычно одну, если логика "одна конфигурация на компанию") конфигураций Telegram для компании текущего пользователя.
- **Тело ответа (JSON):** `List<IntegrationTelegramDto>`
```json
[
  {
    "id": 1,
    "companyId": 101,
    "botUsername": "my_company_bot",
    "createdAt": "2023-10-28T10:00:00Z",
    "updatedAt": "2023-10-28T10:05:00Z"
  }
]
```

### 1.3. Получить конфигурацию Telegram по ID

- **Метод:** `GET`
- **Путь:** `/api/ui/integration/telegram/{id}`
- **Описание:** Возвращает детали конфигурации Telegram по ее ID.
- **Параметры пути:** `id` ID конфигурации Telegram.

- **Тело ответа (JSON):** `IntegrationTelegramDto`
```json
{
  "id": 1,
  "companyId": 101,
  "botUsername": "my_company_bot",
  "createdAt": "2023-10-28T10:00:00Z",
  "updatedAt": "2023-10-28T10:05:00Z"
}
```

### 1.4. Удалить конфигурацию Telegram

- **Метод:** `DELETE`
- **Путь:** `/api/ui/integration/telegram/{id}`
- **Описание:** Удаляет конфигурацию Telegram по ее ID.
- **Параметры пути:** `id` ID конфигурации Telegram.


## 2. Интеграция с Email (Почтой)

Эндпоинты для управления конфигурацией интеграции с Email.

### 2.1. Создать или обновить конфигурацию Email

- **Метод:** `POST`
- **Путь:** `/api/ui/integration/mail`
- **Описание:** Создает новую конфигурацию Email для компании текущего пользователя или обновляет существующую.
- **Тело запроса (JSON):** `CreateMailConfigurationRequest`
```json
{
  "emailAddress": "support@mycompany.com",
  "appPassword": "your_app_specific_password"
}
 ```

- **Тело ответа (JSON):** `IntegrationTelegramDto`
```json
{
  "id": 1,
  "companyId": 101,
  "emailAddress": "support@mycompany.com",
  "imapServer": "imap.myprovider.com",
  "smtpServer": "smtp.myprovider.com",
  "imapPort": 993,
  "folder": "INBOX",
  "createdAt": "2023-10-28T11:00:00Z",
  "updatedAt": "2023-10-28T11:05:00Z"
}
```

### 2.2. Получить все конфигурации Email для компании

- **Метод:** `GET`
- **Путь:** `/api/ui/integration/mail`
- **Описание:** Возвращает список конфигураций Email для компании текущего пользователя.
- **Тело ответа (JSON):** `List<IntegrationMailDto>`
```json
[
  {
    "id": 1,
    "companyId": 101,
    "emailAddress": "support@mycompany.com",
    "imapServer": "imap.myprovider.com",
    "smtpServer": "smtp.myprovider.com",
    "imapPort": 993,
    "folder": "INBOX",
    "createdAt": "2023-10-28T11:00:00Z",
    "updatedAt": "2023-10-28T11:05:00Z"
  }
]
```

### 2.3. Получить конфигурацию Email по ID

- **Метод:** `GET`
- **Путь:** `/api/ui/integration/mail/{id}`
- **Описание:** Возвращает детали конфигурации Email по ее ID.
- **Параметры пути:** `id` ID конфигурации Email.

- **Тело ответа (JSON):** `IntegrationMailDto`
```json
{
  "id": 1,
  "companyId": 101,
  "emailAddress": "support@mycompany.com",
  "imapServer": "imap.myprovider.com",
  "smtpServer": "smtp.myprovider.com",
  "imapPort": 993,
  "folder": "INBOX",
  "createdAt": "2023-10-28T11:00:00Z",
  "updatedAt": "2023-10-28T11:05:00Z"
}
```

### 2.4. Удалить конфигурацию Email

- **Метод:** `DELETE`
- **Путь:** `/api/ui/integration/mail/{id}`
- **Описание:** Удаляет конфигурацию Email по ее ID.
- **Параметры пути:** `id` ID конфигурации Email.

## 3. Интеграция с WhatsApp

Эндпоинты для управления конфигурацией интеграции с WhatsApp.

### 3.1. Создать или обновить конфигурацию WhatsApp

- **Метод:** `POST`
- **Путь:** `/api/ui/integration/whatsapp`
- **Описание:** Создает новую конфигурацию WhatsApp для компании текущего пользователя или обновляет существующую.
- **Тело запроса (JSON):** `CreateWhatsappConfigurationRequest`
```json
{
  "phoneNumberId": 123456789012345,
  "accessToken": "EAA...",
  "verifyToken": "my_custom_verify_token"
}
 ```

- **Тело ответа (JSON):** `IntegrationWhatsappDto`
```json
{
  "id": 1,
  "companyId": 101,
  "phoneNumberId": 123456789012345,
  "verifyToken": "my_custom_verify_token",
  "createdAt": "2023-10-28T12:00:00Z",
  "updatedAt": "2023-10-28T12:05:00Z"
}
```

### 3.2. Получить все конфигурации WhatsApp для компании

- **Метод:** `GET`
- **Путь:** `/api/ui/integration/whatsapp`
- **Описание:** Возвращает список конфигураций WhatsApp для компании текущего пользователя.
- **Тело ответа (JSON):** `List<IntegrationWhatsappDto>`
```json
[
  {
    "id": 1,
    "companyId": 101,
    "phoneNumberId": 123456789012345,
    "verifyToken": "my_custom_verify_token",
    "createdAt": "2023-10-28T12:00:00Z",
    "updatedAt": "2023-10-28T12:05:00Z"
  }
]
```

### 3.3. Получить конфигурацию WhatsApp по ID

- **Метод:** `GET`
- **Путь:** `/api/ui/integration/whatsapp/{id}`
- **Описание:** Возвращает детали конфигурации WhatsApp по ее ID.
- **Параметры пути:** `id` ID конфигурации WhatsApp.

- **Тело ответа (JSON):** `IntegrationWhatsappDto`
```json
{
  "id": 1,
  "companyId": 101,
  "phoneNumberId": 123456789012345,
  "verifyToken": "my_custom_verify_token",
  "createdAt": "2023-10-28T12:00:00Z",
  "updatedAt": "2023-10-28T12:05:00Z"
}
```

### 3.4. Удалить конфигурацию WhatsApp

- **Метод:** `DELETE`
- **Путь:** `/api/ui/integration/whatsapp/{id}`
- **Описание:** Удаляет конфигурацию WhatsApp по ее ID.
- **Параметры пути:** `id` ID конфигурации WhatsApp.


## 4. Интеграция с VK (ВКонтакте)

Эндпоинты для управления конфигурацией интеграции с VK.

### 4.1. Создать или обновить конфигурацию VK

- **Метод:** `POST`
- **Путь:** `/api/ui/integration/vk`
- **Описание:** Создает новую конфигурацию VK для компании текущего пользователя или обновляет существующую.
- **Тело запроса (JSON):** `CreateVkConfigurationRequest`
```json
{
  "communityId": 123456789,
  "accessToken": "vk1.a...",
  "communityName": "My Company VK Page"
}
 ```

- **Тело ответа (JSON):** `IntegrationVkDto`
```json
{
  "id": 1,
  "companyId": 101,
  "communityId": 123456789,
  "communityName": "My Company VK Page",
  "createdAt": "2023-10-28T13:00:00Z",
  "updatedAt": "2023-10-28T13:05:00Z",
  "active": true
}
```

### 4.2. Получить все конфигурации VK для компании

- **Метод:** `GET`
- **Путь:** `/api/ui/integration/vk`
- **Описание:** Возвращает список конфигураций VK для компании текущего пользователя.
- **Тело ответа (JSON):** `List<IntegrationVkDto>`
```json
[
  {
    "id": 1,
    "companyId": 101,
    "communityId": 123456789,
    "communityName": "My Company VK Page",
    "createdAt": "2023-10-28T13:00:00Z",
    "updatedAt": "2023-10-28T13:05:00Z",
    "active": true
  }
]
```

### 4.3. Получить конфигурацию VK по ID

- **Метод:** `GET`
- **Путь:** `/api/ui/integration/vk/{id}`
- **Описание:** Возвращает детали конфигурации VK по ее ID.
- **Параметры пути:** `id` ID конфигурации VK.

- **Тело ответа (JSON):** `IntegrationVkDto`
```json
{
  "id": 1,
  "companyId": 101,
  "communityId": 123456789,
  "communityName": "My Company VK Page",
  "createdAt": "2023-10-28T13:00:00Z",
  "updatedAt": "2023-10-28T13:05:00Z",
  "active": true
}
```

### 4.4. Удалить конфигурацию VK

- **Метод:** `DELETE`
- **Путь:** `/api/ui/integration/vk/{id}`
- **Описание:** Удаляет конфигурацию VK по ее ID.
- **Параметры пути:** `id` ID конфигурации VK.
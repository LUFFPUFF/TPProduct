# 📦 Predefined Answers Module API – UI Endpoints Overview

Все эндпоинты пользовательского интерфейса для работы с заранее подготовленными ответами располагаются под общим префиксом:  
**`/api/ui/predefined-answers`**

---

## 📄 Получение всех предустановленных ответов

**GET `/api/ui/predefined-answers`**  
Возвращает список всех предустановленных ответов, доступных текущему пользователю.

**Response Body (JSON):**
```json
[
  {
    "id": 1,
    "title": "Приветствие",
    "answer": "Здравствуйте! Чем могу помочь?",
    "category": "Общие",
    "companyName": "ООО Ромашка",
    "createdAt": "2024-04-01T12:00:00Z"
  }
]
```

---

## ➕ Создание предустановленного ответа

**POST `/api/ui/predefined-answers`**  
Создаёт новый предустановленный ответ.

**Request Body (JSON):**
```json
{
  "title": "Название ответа",
  "answer": "Текст ответа",
  "category": "Категория"
}
```

**Валидация:**
- `title`, `answer`, `category` – обязательные поля, не могут быть пустыми.

**Response Body (JSON):**
```json
{
  "id": 42,
  "title": "Название ответа",
  "answer": "Текст ответа",
  "category": "Категория",
  "companyName": "ООО Ромашка",
  "createdAt": "2024-05-02T10:30:00Z"
}
```

---

## 🗑️ Удаление предустановленного ответа

**DELETE `/api/ui/predefined-answers/{id}`**  
Удаляет предустановленный ответ по его идентификатору.

**Path-параметры:**
- `id` (Integer): ID ответа, который нужно удалить

**Пример:** DELETE /api/ui/predefined-answers/42

---

## 📤 Массовая загрузка ответов

**POST `/api/ui/predefined-answers/upload`**  
Загружает файл с предустановленными ответами. Используется для массового импорта.

**Path-параметры:**
- `file` (MultipartFile): MultipartFile формата TXT, JSON, XML, CSV
- `category` (String): не важный параметр
- `overwrite` (String, defaultValue = "false"): Флаг, указывающий, нужно ли перезаписывать существующие ответы в этой категории.

**Response Body (JSON):**
```json
{
  "processedCount": 25,
  "duplicatesCount": 5,
  "globalErrors": [],
  "rowErrors": {
    "3": "Пустой заголовок",
    "7": "Неверный формат категории"
  },
  "status": "PARTIALLY_IMPORTED"
}
```

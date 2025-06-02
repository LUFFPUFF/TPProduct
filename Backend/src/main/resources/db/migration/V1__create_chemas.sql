-- =====================================================================================
-- Table: company
-- Описание: Хранит информацию о компаниях-клиентах системы.
-- =====================================================================================
CREATE TABLE IF NOT EXISTS company (
                                       id SERIAL PRIMARY KEY,
                                       name VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    company_description TEXT
    );

CREATE INDEX IF NOT EXISTS idx_company_contact_email ON company(contact_email);
CREATE INDEX IF NOT EXISTS idx_company_created_at ON company(created_at);

COMMENT ON TABLE company IS 'Компании-клиенты системы';
COMMENT ON COLUMN company.id IS 'Уникальный идентификатор компании';
COMMENT ON COLUMN company.name IS 'Название компании';
COMMENT ON COLUMN company.contact_email IS 'Контактный email компании';
COMMENT ON COLUMN company.created_at IS 'Время создания записи о компании';
COMMENT ON COLUMN company.updated_at IS 'Время последнего обновления записи о компании';
COMMENT ON COLUMN company.company_description IS 'Описание компании';

-- =====================================================================================
-- Table: subscription
-- Описание: Хранит информацию о подписках компаний.
-- =====================================================================================
CREATE TABLE IF NOT EXISTS subscription (
                                            id SERIAL PRIMARY KEY,
                                            company_id INT NOT NULL UNIQUE,
                                            status VARCHAR(255) NOT NULL,
    cost REAL NOT NULL,
    count_operators INT NOT NULL,
    max_operators INT NOT NULL,
    start_subscription TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_subscription TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_subscription_company
    FOREIGN KEY (company_id)
    REFERENCES company (id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_subscription_company_id ON subscription(company_id);
CREATE INDEX IF NOT EXISTS idx_subscription_status ON subscription(status);
CREATE INDEX IF NOT EXISTS idx_subscription_start_subscription ON subscription(start_subscription);
CREATE INDEX IF NOT EXISTS idx_subscription_end_subscription ON subscription(end_subscription);

COMMENT ON TABLE subscription IS 'Подписки компаний на сервисы';
COMMENT ON COLUMN subscription.id IS 'Уникальный идентификатор подписки';
COMMENT ON COLUMN subscription.company_id IS 'Ссылка на компанию, которой принадлежит подписка (один к одному)';
COMMENT ON COLUMN subscription.status IS 'Статус подписки (ACTIVE, EXPIRED, CANCELLED)';
COMMENT ON COLUMN subscription.cost IS 'Стоимость подписки';
COMMENT ON COLUMN subscription.count_operators IS 'Текущее количество операторов по подписке';
COMMENT ON COLUMN subscription.max_operators IS 'Максимальное количество операторов по подписке';
COMMENT ON COLUMN subscription.start_subscription IS 'Дата и время начала действия подписки';
COMMENT ON COLUMN subscription.end_subscription IS 'Дата и время окончания действия подписки';
COMMENT ON COLUMN subscription.created_at IS 'Время создания записи о подписке';
COMMENT ON COLUMN subscription.updated_at IS 'Время последнего обновления записи о подписке';

-- =====================================================================================
-- Table: users
-- Описание: Хранит информацию о пользователях системы (операторы, менеджеры).
-- =====================================================================================
CREATE TABLE IF NOT EXISTS users (
                                     id SERIAL PRIMARY KEY,
                                     company_id INT,
                                     full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(255),
    date_of_birth TIMESTAMP WITHOUT TIME ZONE,
    gender VARCHAR(255),
    password VARCHAR(255),
    profile_picture VARCHAR(1024),
    max_concurrent_chats INT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE,

    CONSTRAINT fk_users_company
    FOREIGN KEY (company_id)
    REFERENCES company (id) ON DELETE SET NULL
    );

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);
CREATE INDEX IF NOT EXISTS idx_users_company_id ON users(company_id);

COMMENT ON TABLE users IS 'Пользователи системы (операторы, менеджеры и т.д.)';
COMMENT ON COLUMN users.id IS 'Уникальный идентификатор пользователя';
COMMENT ON COLUMN users.company_id IS 'Ссылка на компанию, к которой принадлежит пользователь (может быть NULL)';
COMMENT ON COLUMN users.full_name IS 'Полное имя пользователя';
COMMENT ON COLUMN users.email IS 'Email пользователя (уникальный)';
COMMENT ON COLUMN users.status IS 'Статус пользователя (ACTIVE, INACTIVE, BLOCKED)';
COMMENT ON COLUMN users.date_of_birth IS 'Дата рождения пользователя';
COMMENT ON COLUMN users.gender IS 'Пол пользователя (MALE, FEMALE)';
COMMENT ON COLUMN users.password IS 'Хэш пароля пользователя';
COMMENT ON COLUMN users.profile_picture IS 'URL или путь к аватару пользователя';
COMMENT ON COLUMN users.max_concurrent_chats IS 'Максимальное количество одновременных чатов для оператора';
COMMENT ON COLUMN users.created_at IS 'Время создания записи о пользователе';
COMMENT ON COLUMN users.updated_at IS 'Время последнего обновления записи о пользователе';

-- =====================================================================================
-- Table: user_roles
-- Описание: Связывает пользователей с их ролями в системе.
-- =====================================================================================
CREATE TABLE IF NOT EXISTS user_roles (
                                          id SERIAL PRIMARY KEY,
                                          user_id INT NOT NULL,
                                          role VARCHAR(255) NOT NULL,

    CONSTRAINT fk_user_roles_user
    FOREIGN KEY (user_id)
    REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_user_roles_user_role UNIQUE (user_id, role)
    );

CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role ON user_roles(role);


COMMENT ON TABLE user_roles IS 'Роли пользователей в системе';
COMMENT ON COLUMN user_roles.id IS 'Уникальный идентификатор связи пользователь-роль';
COMMENT ON COLUMN user_roles.user_id IS 'Ссылка на пользователя';
COMMENT ON COLUMN user_roles.role IS 'Роль пользователя (MANAGER, OPERATOR, USER)';

-- =====================================================================================
-- Table: clients
-- Описание: Хранит информацию о клиентах компаний.
-- =====================================================================================
CREATE TABLE IF NOT EXISTS clients (
                                       id SERIAL PRIMARY KEY,
                                       user_id INT,
                                       company_id INT NOT NULL,
                                       name VARCHAR(255),
    type VARCHAR(255),
    tag VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_clients_user
    FOREIGN KEY (user_id)
    REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_clients_company
    FOREIGN KEY (company_id)
    REFERENCES company (id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_clients_user_id ON clients(user_id);
CREATE INDEX IF NOT EXISTS idx_clients_company_id ON clients(company_id);
CREATE INDEX IF NOT EXISTS idx_clients_type ON clients(type);
CREATE INDEX IF NOT EXISTS idx_clients_tag ON clients(tag);
CREATE INDEX IF NOT EXISTS idx_clients_name ON clients(name);

COMMENT ON TABLE clients IS 'Клиенты компаний';
COMMENT ON COLUMN clients.id IS 'Уникальный идентификатор клиента';
COMMENT ON COLUMN clients.user_id IS 'Ссылка на пользователя (оператора), ответственного за клиента';
COMMENT ON COLUMN clients.company_id IS 'Ссылка на компанию, к которой принадлежит клиент';
COMMENT ON COLUMN clients.name IS 'Имя клиента';
COMMENT ON COLUMN clients.type IS 'Тип клиента (IMPORTANT, PROBLEMATIC)';
COMMENT ON COLUMN clients.tag IS 'Тэг клиента для дополнительной классификации';
COMMENT ON COLUMN clients.created_at IS 'Время создания записи о клиенте';
COMMENT ON COLUMN clients.updated_at IS 'Время последнего обновления записи о клиенте';

-- =====================================================================================
-- Table: client_contacts
-- Описание: Хранит контактную информацию клиентов.
-- =====================================================================================
CREATE TABLE IF NOT EXISTS client_contacts (
                                               id SERIAL PRIMARY KEY,
                                               client_id INT NOT NULL,
                                               type VARCHAR(255),
    value VARCHAR(255),
    company_id INT,

    CONSTRAINT fk_client_contacts_client
    FOREIGN KEY (client_id)
    REFERENCES clients (id) ON DELETE CASCADE,
    CONSTRAINT fk_client_contacts_company
    FOREIGN KEY (company_id)
    REFERENCES company (id) ON DELETE SET NULL
    );

CREATE INDEX IF NOT EXISTS idx_client_contacts_client_id ON client_contacts(client_id);
CREATE INDEX IF NOT EXISTS idx_client_contacts_type ON client_contacts(type);
CREATE INDEX IF NOT EXISTS idx_client_contacts_value ON client_contacts(value);
CREATE INDEX IF NOT EXISTS idx_client_contacts_company_id ON client_contacts(company_id);

COMMENT ON TABLE client_contacts IS 'Контактная информация клиентов';
COMMENT ON COLUMN client_contacts.id IS 'Уникальный идентификатор контакта';
COMMENT ON COLUMN client_contacts.client_id IS 'Ссылка на клиента, которому принадлежит контакт';
COMMENT ON COLUMN client_contacts.type IS 'Тип контакта (EMAIL, PHONE, и т.д.)';
COMMENT ON COLUMN client_contacts.value IS 'Значение контакта (адрес email, номер телефона)';
COMMENT ON COLUMN client_contacts.company_id IS 'Ссылка на компанию (если контакт специфичен для контекста компании)';

-- =====================================================================================
-- Table: client_notes
-- Описание: Хранит заметки по клиентам.
-- =====================================================================================
CREATE TABLE IF NOT EXISTS client_notes (
                                            id SERIAL PRIMARY KEY,
                                            client_id INT NOT NULL,
                                            note TEXT,
                                            created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                            CONSTRAINT fk_client_notes_client
                                            FOREIGN KEY (client_id)
    REFERENCES clients (id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_client_notes_client_id ON client_notes(client_id);
CREATE INDEX IF NOT EXISTS idx_client_notes_created_at ON client_notes(created_at);

COMMENT ON TABLE client_notes IS 'Заметки по клиентам';
COMMENT ON COLUMN client_notes.id IS 'Уникальный идентификатор заметки';
COMMENT ON COLUMN client_notes.client_id IS 'Ссылка на клиента, к которому относится заметка';
COMMENT ON COLUMN client_notes.note IS 'Текст заметки';
COMMENT ON COLUMN client_notes.created_at IS 'Время создания заметки';

-- =====================================================================================
-- Table: deal_stages
-- Описание: Справочник стадий сделок.
-- =====================================================================================
CREATE TABLE IF NOT EXISTS deal_stages (
                                           id INT PRIMARY KEY,
                                           name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    order_index INT UNIQUE
    );

CREATE INDEX IF NOT EXISTS idx_deal_stages_order_index ON deal_stages(order_index);

COMMENT ON TABLE deal_stages IS 'Справочник стадий сделок (воронка продаж)';
COMMENT ON COLUMN deal_stages.id IS 'Уникальный идентификатор стадии сделки (предполагается, что не автоинкрементный)';
COMMENT ON COLUMN deal_stages.name IS 'Название стадии сделки (уникальное)';
COMMENT ON COLUMN deal_stages.description IS 'Описание стадии сделки';
COMMENT ON COLUMN deal_stages.order_index IS 'Порядковый номер стадии для сортировки (уникальный)';

-- =====================================================================================
-- Table: deals
-- Описание: Хранит информацию о сделках с клиентами.
-- =====================================================================================
CREATE TABLE IF NOT EXISTS deals (
                                     id SERIAL PRIMARY KEY,
                                     client_id INT,
                                     user_id INT,
                                     stage_id INT,
                                     content TEXT,
                                     amount REAL,
                                     status VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_deals_client
    FOREIGN KEY (client_id)
    REFERENCES clients (id) ON DELETE SET NULL,
    CONSTRAINT fk_deals_user
    FOREIGN KEY (user_id)
    REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_deals_stage
    FOREIGN KEY (stage_id)
    REFERENCES deal_stages (id) ON DELETE RESTRICT
    );

CREATE INDEX IF NOT EXISTS idx_deals_client_id ON deals(client_id);
CREATE INDEX IF NOT EXISTS idx_deals_user_id ON deals(user_id);
CREATE INDEX IF NOT EXISTS idx_deals_stage_id ON deals(stage_id);
CREATE INDEX IF NOT EXISTS idx_deals_status ON deals(status);
CREATE INDEX IF NOT EXISTS idx_deals_created_at ON deals(created_at);

COMMENT ON TABLE deals IS 'Сделки с клиентами';
COMMENT ON COLUMN deals.id IS 'Уникальный идентификатор сделки';
COMMENT ON COLUMN deals.client_id IS 'Ссылка на клиента, с которым связана сделка';
COMMENT ON COLUMN deals.user_id IS 'Ссылка на пользователя, ответственного за сделку';
COMMENT ON COLUMN deals.stage_id IS 'Ссылка на текущую стадию сделки';
COMMENT ON COLUMN deals.content IS 'Описание или детали сделки';
COMMENT ON COLUMN deals.amount IS 'Сумма сделки';
COMMENT ON COLUMN deals.status IS 'Статус сделки (OPENED, CLOSED)';
COMMENT ON COLUMN deals.created_at IS 'Время создания сделки';

-- =====================================================================================
-- Table: tasks
-- Описание: Хранит задачи, связанные со сделками или пользователями.
-- =====================================================================================
CREATE TABLE IF NOT EXISTS tasks (
                                     id SERIAL PRIMARY KEY,
                                     deal_id INT,
                                     user_id INT,
                                     title VARCHAR(255),
    status VARCHAR(255),
    priority VARCHAR(255),
    due_date TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_tasks_deal
    FOREIGN KEY (deal_id)
    REFERENCES deals (id) ON DELETE CASCADE,
    CONSTRAINT fk_tasks_user
    FOREIGN KEY (user_id)
    REFERENCES users (id) ON DELETE SET NULL
    );

CREATE INDEX IF NOT EXISTS idx_tasks_deal_id ON tasks(deal_id);
CREATE INDEX IF NOT EXISTS idx_tasks_user_id ON tasks(user_id);
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);
CREATE INDEX IF NOT EXISTS idx_tasks_priority ON tasks(priority);
CREATE INDEX IF NOT EXISTS idx_tasks_due_date ON tasks(due_date);
CREATE INDEX IF NOT EXISTS idx_tasks_created_at ON tasks(created_at);

COMMENT ON TABLE tasks IS 'Задачи по сделкам или для пользователей';
COMMENT ON COLUMN tasks.id IS 'Уникальный идентификатор задачи';
COMMENT ON COLUMN tasks.deal_id IS 'Ссылка на сделку, с которой связана задача (может быть NULL)';
COMMENT ON COLUMN tasks.user_id IS 'Ссылка на пользователя, которому назначена задача';
COMMENT ON COLUMN tasks.title IS 'Название или краткое описание задачи';
COMMENT ON COLUMN tasks.status IS 'Статус задачи (OPENED, CLOSED)';
COMMENT ON COLUMN tasks.priority IS 'Приоритет задачи (LOW, MEDIUM, HIGH)';
COMMENT ON COLUMN tasks.due_date IS 'Срок выполнения задачи';
COMMENT ON COLUMN tasks.created_at IS 'Время создания задачи';

-- =====================================================================================
-- Table: company_dialogx_chat_configuration
-- Описание: Конфигурация DialogX чата для компании.
-- =====================================================================================
CREATE TABLE IF NOT EXISTS company_dialogx_chat_configuration (
                                                                  id SERIAL PRIMARY KEY,
                                                                  company_id INT NOT NULL UNIQUE,
                                                                  widget_id VARCHAR(255) NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    welcome_message VARCHAR(500) DEFAULT 'Привет! Чем могу помочь?',
    theme_color VARCHAR(20) DEFAULT '#5A38D9',
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_dialogx_chat_config_company
    FOREIGN KEY (company_id)
    REFERENCES company (id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_dialogx_chat_config_company_id_non_unique ON company_dialogx_chat_configuration(company_id);

COMMENT ON TABLE company_dialogx_chat_configuration IS 'Конфигурация DialogX чата для компании';
COMMENT ON COLUMN company_dialogx_chat_configuration.id IS 'Уникальный идентификатор конфигурации';
COMMENT ON COLUMN company_dialogx_chat_configuration.company_id IS 'Ссылка на компанию (один к одному)';
COMMENT ON COLUMN company_dialogx_chat_configuration.widget_id IS 'Уникальный идентификатор виджета DialogX';
COMMENT ON COLUMN company_dialogx_chat_configuration.enabled IS 'Включена ли конфигурация (true/false)';
COMMENT ON COLUMN company_dialogx_chat_configuration.welcome_message IS 'Приветственное сообщение в чате';
COMMENT ON COLUMN company_dialogx_chat_configuration.theme_color IS 'Основной цвет темы виджета (HEX)';
COMMENT ON COLUMN company_dialogx_chat_configuration.created_at IS 'Время создания конфигурации';
COMMENT ON COLUMN company_dialogx_chat_configuration.updated_at IS 'Время последнего обновления конфигурации';

-- =====================================================================================
-- Table: company_mail_configuration
-- Описание: Конфигурация почты для компании.
-- =====================================================================================
CREATE TABLE IF NOT EXISTS company_mail_configuration (
                                                          id SERIAL PRIMARY KEY,
                                                          company_id INT NOT NULL,
                                                          email_address VARCHAR(255) NOT NULL UNIQUE,
    app_password VARCHAR(255) NOT NULL,
    imap_server VARCHAR(255) NOT NULL,
    smtp_server VARCHAR(255),
    imap_port INT NOT NULL DEFAULT 993,
    folder VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_mail_config_company
    FOREIGN KEY (company_id)
    REFERENCES company (id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_mail_config_company_id ON company_mail_configuration(company_id);

COMMENT ON TABLE company_mail_configuration IS 'Конфигурация почтовых ящиков для интеграции с компаниями';
COMMENT ON COLUMN company_mail_configuration.id IS 'Уникальный идентификатор конфигурации почты';
COMMENT ON COLUMN company_mail_configuration.company_id IS 'Ссылка на компанию';
COMMENT ON COLUMN company_mail_configuration.email_address IS 'Адрес электронной почты (уникальный)';
COMMENT ON COLUMN company_mail_configuration.app_password IS 'Пароль приложения для доступа к почте (зашифрованный)';
COMMENT ON COLUMN company_mail_configuration.imap_server IS 'Адрес IMAP сервера';
COMMENT ON COLUMN company_mail_configuration.smtp_server IS 'Адрес SMTP сервера (если используется для отправки)';
COMMENT ON COLUMN company_mail_configuration.imap_port IS 'Порт IMAP сервера';
COMMENT ON COLUMN company_mail_configuration.folder IS 'Папка для мониторинга писем (например, INBOX)';
COMMENT ON COLUMN company_mail_configuration.created_at IS 'Время создания конфигурации';
COMMENT ON COLUMN company_mail_configuration.updated_at IS 'Время последнего обновления конфигурации';

-- =====================================================================================
-- Table: company_telegram_configuration
-- Описание: Конфигурация Telegram бота для компании.
-- =====================================================================================
CREATE TABLE IF NOT EXISTS company_telegram_configuration (
                                                              id SERIAL PRIMARY KEY,
                                                              chat_telegram_id BIGINT,
                                                              company_id INT NOT NULL,
                                                              bot_username VARCHAR(255) NOT NULL,
    bot_token VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_telegram_config_company
    FOREIGN KEY (company_id)
    REFERENCES company (id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_telegram_config_company_id ON company_telegram_configuration(company_id);
CREATE INDEX IF NOT EXISTS idx_telegram_config_bot_username ON company_telegram_configuration(bot_username);

COMMENT ON TABLE company_telegram_configuration IS 'Конфигурация Telegram ботов для компаний';
COMMENT ON COLUMN company_telegram_configuration.id IS 'Уникальный идентификатор конфигурации Telegram';
COMMENT ON COLUMN company_telegram_configuration.chat_telegram_id IS 'ID Telegram чата (если используется)';
COMMENT ON COLUMN company_telegram_configuration.company_id IS 'Ссылка на компанию';
COMMENT ON COLUMN company_telegram_configuration.bot_username IS 'Имя пользователя Telegram бота';
COMMENT ON COLUMN company_telegram_configuration.bot_token IS 'Токен доступа Telegram бота (зашифрованный)';
COMMENT ON COLUMN company_telegram_configuration.created_at IS 'Время создания конфигурации';
COMMENT ON COLUMN company_telegram_configuration.updated_at IS 'Время последнего обновления конфигурации';

-- =====================================================================================
-- Table: company_vk_configurations
-- Описание: Конфигурация VK сообщества для компании.
-- =====================================================================================
CREATE TABLE IF NOT EXISTS company_vk_configurations (
                                                         id SERIAL PRIMARY KEY,
                                                         company_id INT NOT NULL,
                                                         community_id BIGINT NOT NULL UNIQUE,
                                                         access_token VARCHAR(255) NOT NULL,
    community_name VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_vk_config_company
    FOREIGN KEY (company_id)
    REFERENCES company (id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_vk_config_company_id ON company_vk_configurations(company_id);


COMMENT ON TABLE company_vk_configurations IS 'Конфигурация VK сообществ для компаний';
COMMENT ON COLUMN company_vk_configurations.id IS 'Уникальный идентификатор конфигурации VK';
COMMENT ON COLUMN company_vk_configurations.company_id IS 'Ссылка на компанию';
COMMENT ON COLUMN company_vk_configurations.community_id IS 'ID VK сообщества (уникальный)';
COMMENT ON COLUMN company_vk_configurations.access_token IS 'Токен доступа к VK API (зашифрованный)';
COMMENT ON COLUMN company_vk_configurations.community_name IS 'Название VK сообщества';
COMMENT ON COLUMN company_vk_configurations.created_at IS 'Время создания конфигурации';
COMMENT ON COLUMN company_vk_configurations.updated_at IS 'Время последнего обновления конфигурации';

-- =====================================================================================
-- Table: company_whatsapp_configurations
-- Описание: Конфигурация WhatsApp для компании.
-- =====================================================================================
CREATE TABLE IF NOT EXISTS company_whatsapp_configurations (
                                                               id SERIAL PRIMARY KEY,
                                                               company_id INT NOT NULL,
                                                               phone_number_id BIGINT NOT NULL UNIQUE,
                                                               access_token VARCHAR(1024) NOT NULL,
    verify_token VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_whatsapp_config_company
    FOREIGN KEY (company_id)
    REFERENCES company (id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_whatsapp_config_company_id ON company_whatsapp_configurations(company_id);

COMMENT ON TABLE company_whatsapp_configurations IS 'Конфигурация WhatsApp Business API для компаний';
COMMENT ON COLUMN company_whatsapp_configurations.id IS 'Уникальный идентификатор конфигурации WhatsApp';
COMMENT ON COLUMN company_whatsapp_configurations.company_id IS 'Ссылка на компанию';
COMMENT ON COLUMN company_whatsapp_configurations.phone_number_id IS 'ID номера телефона WhatsApp (уникальный)';
COMMENT ON COLUMN company_whatsapp_configurations.access_token IS 'Токен доступа WhatsApp (зашифрованный)';
COMMENT ON COLUMN company_whatsapp_configurations.verify_token IS 'Токен для верификации webhook';
COMMENT ON COLUMN company_whatsapp_configurations.created_at IS 'Время создания конфигурации';
COMMENT ON COLUMN company_whatsapp_configurations.updated_at IS 'Время последнего обновления конфигурации';

-- =====================================================================================
-- Table: chats
-- Описание: Хранит информацию о чатах.
-- =====================================================================================
CREATE TABLE IF NOT EXISTS chats (
                                     id SERIAL PRIMARY KEY,
                                     external_chat_id VARCHAR(255),
    company_id INT NOT NULL,
    client_id INT NOT NULL,
    user_id INT,
    chat_channel VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    has_operator_responded BOOLEAN,
    awaiting_feedback_for_ai_response_id INT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    assigned_at TIMESTAMP WITHOUT TIME ZONE,
    closed_at TIMESTAMP WITHOUT TIME ZONE,
    last_message_at TIMESTAMP WITHOUT TIME ZONE,

    CONSTRAINT fk_chats_company
    FOREIGN KEY (company_id)
    REFERENCES companies (id) ON DELETE CASCADE,
    CONSTRAINT fk_chats_client
    FOREIGN KEY (client_id)
    REFERENCES clients (id) ON DELETE CASCADE,
    CONSTRAINT fk_chats_user
    FOREIGN KEY (user_id)
    REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chats_chat_channel_check CHECK (chat_channel IN ('VK', 'Telegram', 'Email', 'WhatsApp'))
    );

CREATE INDEX IF NOT EXISTS idx_chats_status ON chats(status);
CREATE INDEX IF NOT EXISTS idx_chats_created_at ON chats(created_at);
CREATE INDEX IF NOT EXISTS idx_chats_company_id ON chats(company_id);
CREATE INDEX IF NOT EXISTS idx_chats_client_id ON chats(client_id);
CREATE INDEX IF NOT EXISTS idx_chats_user_id ON chats(user_id);
CREATE INDEX IF NOT EXISTS idx_chats_chat_channel ON chats(chat_channel);


COMMENT ON TABLE chats IS 'Чаты с клиентами';
COMMENT ON COLUMN chats.id IS 'Уникальный идентификатор чата';
COMMENT ON COLUMN chats.external_chat_id IS 'Внешний идентификатор чата из системы-источника';
COMMENT ON COLUMN chats.company_id IS 'Ссылка на компанию, к которой относится чат';
COMMENT ON COLUMN chats.client_id IS 'Ссылка на клиента, участвующего в чате';
COMMENT ON COLUMN chats.user_id IS 'Ссылка на оператора, назначенного на чат (может быть NULL)';
COMMENT ON COLUMN chats.chat_channel IS 'Канал общения (VK, Telegram, Email, WhatsApp и т.д.)';
COMMENT ON COLUMN chats.status IS 'Текущий статус чата (NEW, PENDING_OPERATOR, ASSIGNED, CLOSED и т.д.)';
COMMENT ON COLUMN chats.has_operator_responded IS 'Ответил ли оператор в этом чате';
COMMENT ON COLUMN chats.awaiting_feedback_for_ai_response_id IS 'ID ответа ИИ, для которого ожидается обратная связь';
COMMENT ON COLUMN chats.created_at IS 'Время создания чата';
COMMENT ON COLUMN chats.assigned_at IS 'Время назначения оператора на чат';
COMMENT ON COLUMN chats.closed_at IS 'Время закрытия чата';
COMMENT ON COLUMN chats.last_message_at IS 'Время последнего сообщения в чате';

-- =====================================================================================
-- Table: chat_messages
-- Описание: Хранит сообщения в рамках чатов.
-- =====================================================================================
CREATE TABLE IF NOT EXISTS chat_messages (
                                             id SERIAL PRIMARY KEY,
                                             chat_id INT NOT NULL,
                                             sender_client_id INT,
                                             sender_user_id INT,
                                             content TEXT,
                                             status VARCHAR(50),
    sender_type VARCHAR(50) NOT NULL,
    external_message_id VARCHAR(255),
    reply_to_external_message_id VARCHAR(255),
    sent_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT fk_chat_messages_chat
    FOREIGN KEY (chat_id)
    REFERENCES chats (id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_messages_sender_client
    FOREIGN KEY (sender_client_id)
    REFERENCES clients (id) ON DELETE SET NULL,
    CONSTRAINT fk_chat_messages_sender_user
    FOREIGN KEY (sender_user_id)
    REFERENCES users (id) ON DELETE SET NULL
    );

CREATE INDEX IF NOT EXISTS idx_chat_messages_chat_id ON chat_messages(chat_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_sent_at ON chat_messages(sent_at);
CREATE INDEX IF NOT EXISTS idx_chat_messages_sender_type ON chat_messages(sender_type);
CREATE INDEX IF NOT EXISTS idx_chat_messages_status ON chat_messages(status);


COMMENT ON TABLE chat_messages IS 'Сообщения в чатах';
COMMENT ON COLUMN chat_messages.id IS 'Уникальный идентификатор сообщения';
COMMENT ON COLUMN chat_messages.chat_id IS 'Ссылка на чат, к которому относится сообщение';
COMMENT ON COLUMN chat_messages.sender_client_id IS 'Ссылка на клиента-отправителя (если отправитель клиент)';
COMMENT ON COLUMN chat_messages.sender_user_id IS 'Ссылка на оператора-отправителя (если отправитель оператор)';
COMMENT ON COLUMN chat_messages.content IS 'Текст сообщения (до 4000 символов)';
COMMENT ON COLUMN chat_messages.status IS 'Статус сообщения (SENT, DELIVERED, READ, FAILED)';
COMMENT ON COLUMN chat_messages.sender_type IS 'Тип отправителя (CLIENT, OPERATOR, AUTO_RESPONDER)';
COMMENT ON COLUMN chat_messages.external_message_id IS 'Внешний идентификатор сообщения из системы-источника';
COMMENT ON COLUMN chat_messages.reply_to_external_message_id IS 'Внешний ID сообщения, на которое это является ответом';
COMMENT ON COLUMN chat_messages.sent_at IS 'Время отправки сообщения';

-- =====================================================================================
-- Table: chat_attachments
-- Описание: Хранит вложения к сообщениям в чатах.
-- =====================================================================================
CREATE TABLE IF NOT EXISTS chat_attachments (
                                                id SERIAL PRIMARY KEY,
                                                chat_message_id INT NOT NULL,
                                                file_url VARCHAR(1024) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size BIGINT,

    CONSTRAINT fk_chat_attachments_chat_message
    FOREIGN KEY (chat_message_id)
    REFERENCES chat_messages (id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_chat_attachments_chat_message_id ON chat_attachments(chat_message_id);
CREATE INDEX IF NOT EXISTS idx_chat_attachments_file_type ON chat_attachments(file_type);

COMMENT ON TABLE chat_attachments IS 'Вложения к сообщениям в чатах';
COMMENT ON COLUMN chat_attachments.id IS 'Уникальный идентификатор вложения';
COMMENT ON COLUMN chat_attachments.chat_message_id IS 'Ссылка на сообщение, к которому относится вложение';
COMMENT ON COLUMN chat_attachments.file_url IS 'URL или путь к файлу вложения';
COMMENT ON COLUMN chat_attachments.file_type IS 'Тип файла (например, image/jpeg, application/pdf)';
COMMENT ON COLUMN chat_attachments.file_size IS 'Размер файла в байтах';

-- =====================================================================================
-- Table: notifications
-- Описание: Хранит уведомления для пользователей системы.
-- =====================================================================================
CREATE TABLE IF NOT EXISTS notifications (
                                             id SERIAL PRIMARY KEY,
                                             user_id INT NOT NULL,
                                             chat_id INT,
                                             type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_notifications_user
    FOREIGN KEY (user_id)
    REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_chat
    FOREIGN KEY (chat_id)
    REFERENCES chats (id) ON DELETE SET NULL
    );

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_chat_id ON notifications(chat_id);
CREATE INDEX IF NOT EXISTS idx_notifications_is_read ON notifications(is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications(type);


COMMENT ON TABLE notifications IS 'Уведомления для пользователей системы';
COMMENT ON COLUMN notifications.id IS 'Уникальный идентификатор уведомления';
COMMENT ON COLUMN notifications.user_id IS 'Ссылка на пользователя, которому предназначено уведомление';
COMMENT ON COLUMN notifications.chat_id IS 'Ссылка на чат, связанный с уведомлением (если применимо)';
COMMENT ON COLUMN notifications.type IS 'Тип уведомления (например, NEW_MESSAGE, CHAT_ASSIGNED)';
COMMENT ON COLUMN notifications.message IS 'Текст уведомления';
COMMENT ON COLUMN notifications.created_at IS 'Время создания уведомления';
COMMENT ON COLUMN notifications.is_read IS 'Прочитано ли уведомление пользователем (true/false)';


-- =====================================================================================
-- Table: ai_responses
-- Описание: Хранит ответы, сгенерированные ИИ.
-- =====================================================================================

CREATE TABLE IF NOT EXISTS ai_responses (
                                            id SERIAL PRIMARY KEY,
                                            chat_message_id INT NOT NULL,
                                            client_id INT NOT NULL,
                                            chat_id INT NOT NULL,
                                            response_text TEXT,
                                            confidence REAL,
                                            created_at TIMESTAMP WITHOUT TIME ZONE,

                                            CONSTRAINT fk_ai_responses_chat_message
                                            FOREIGN KEY (chat_message_id)
    REFERENCES chat_messages (id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_responses_client
    FOREIGN KEY (client_id)
    REFERENCES clients (id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_responses_chat
    FOREIGN KEY (chat_id)
    REFERENCES chats (id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_ai_responses_chat_message_id ON ai_responses(chat_message_id);
CREATE INDEX IF NOT EXISTS idx_ai_responses_client_id ON ai_responses(client_id);
CREATE INDEX IF NOT EXISTS idx_ai_responses_chat_id ON ai_responses(chat_id);

COMMENT ON TABLE ai_responses IS 'Ответы, сгенерированные Искусственным Интеллектом';
COMMENT ON COLUMN ai_responses.id IS 'Уникальный идентификатор ответа ИИ';
COMMENT ON COLUMN ai_responses.chat_message_id IS 'Ссылка на сообщение в чате, на которое был дан ответ ИИ';
COMMENT ON COLUMN ai_responses.client_id IS 'Ссылка на клиента, связанного с этим ответом';
COMMENT ON COLUMN ai_responses.chat_id IS 'Ссылка на чат, в котором был дан ответ';
COMMENT ON COLUMN ai_responses.response_text IS 'Текст ответа, сгенерированного ИИ';
COMMENT ON COLUMN ai_responses.confidence IS 'Уверенность ИИ в сгенерированном ответе';
COMMENT ON COLUMN ai_responses.created_at IS 'Время создания записи об ответе ИИ';


-- =====================================================================================
-- Table: ai_feedback
-- Описание: Хранит обратную связь от клиентов по ответам ИИ.
-- =====================================================================================
CREATE TABLE IF NOT EXISTS ai_feedback (
                                           id SERIAL PRIMARY KEY,
                                           ai_response_id INT NOT NULL,
                                           client_id INT NOT NULL,
                                           rating INT,
                                           comment TEXT,
                                           created_at TIMESTAMP WITHOUT TIME ZONE,

                                           CONSTRAINT fk_ai_feedback_ai_response
                                           FOREIGN KEY (ai_response_id)
    REFERENCES ai_responses (id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_feedback_client
    FOREIGN KEY (client_id)
    REFERENCES clients (id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_ai_feedback_ai_response_id ON ai_feedback(ai_response_id);
CREATE INDEX IF NOT EXISTS idx_ai_feedback_client_id ON ai_feedback(client_id);

COMMENT ON TABLE ai_feedback IS 'Обратная связь от клиентов по ответам Искусственного Интеллекта';
COMMENT ON COLUMN ai_feedback.id IS 'Уникальный идентификатор обратной связи';
COMMENT ON COLUMN ai_feedback.ai_response_id IS 'Ссылка на ответ ИИ, по которому дана обратная связь';
COMMENT ON COLUMN ai_feedback.client_id IS 'Ссылка на клиента, оставившего обратную связь';
COMMENT ON COLUMN ai_feedback.rating IS 'Оценка ответа ИИ (например, от 1 до 5)';
COMMENT ON COLUMN ai_feedback.comment IS 'Текстовый комментарий к обратной связи';
COMMENT ON COLUMN ai_feedback.created_at IS 'Время создания записи обратной связи';

-- =====================================================================================
-- Table: predefined_answers
-- Описание: Хранит предопределенные ответы для использования операторами или ИИ.
-- =====================================================================================
CREATE TABLE IF NOT EXISTS predefined_answers (
                                                  id SERIAL PRIMARY KEY,
                                                  company_id INT NOT NULL,
                                                  category VARCHAR(255),
    title VARCHAR(255),
    answer TEXT,
    trust_score REAL DEFAULT 0.0,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_predefined_answers_company
    FOREIGN KEY (company_id)
    REFERENCES companies (id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_predefined_answers_category ON predefined_answers(category);

COMMENT ON TABLE predefined_answers IS 'Предопределенные (шаблонные) ответы';
COMMENT ON COLUMN predefined_answers.id IS 'Уникальный идентификатор предопределенного ответа';
COMMENT ON COLUMN predefined_answers.company_id IS 'Ссылка на компанию, к которой относится ответ';
COMMENT ON COLUMN predefined_answers.category IS 'Категория предопределенного ответа';
COMMENT ON COLUMN predefined_answers.title IS 'Заголовок/Краткое описание предопределенного ответа';
COMMENT ON COLUMN predefined_answers.answer IS 'Текст предопределенного ответа';
COMMENT ON COLUMN predefined_answers.trust_score IS 'Оценка доверия к этому ответу (например, для ИИ)';
COMMENT ON COLUMN predefined_answers.created_at IS 'Время создания предопределенного ответа';


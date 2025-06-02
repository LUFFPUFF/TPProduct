ALTER TABLE chats
DROP CONSTRAINT IF EXISTS chats_chat_channel_check;

ALTER TABLE chats
    ADD CONSTRAINT chats_chat_channel_check
        CHECK (chat_channel IN (
                                'VK',
                                'Telegram',
                                'Email',
                                'WhatsApp',
                                'DialogX_Chat',
                                'Test',
                                'UNKNOWN'
            ));
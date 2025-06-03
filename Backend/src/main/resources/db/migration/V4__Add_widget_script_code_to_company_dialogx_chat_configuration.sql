ALTER TABLE company_dialogx_chat_configuration
ADD COLUMN widget_script_code TEXT;

UPDATE company_dialogx_chat_configuration
SET widget_script_code = '<script src="https://dialogx.ru/dialogx-widget.js"></script>'
WHERE id IS NOT NULL;
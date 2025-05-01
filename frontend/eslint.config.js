import js from '@eslint/js';
import globals from 'globals';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';

export default [
  // Игнорируем папку dist
  { ignores: ['dist'] },

  // Настройки для файлов JavaScript/JSX
  {
    files: ['**/*.{js,jsx}'], // Применяем конфигурацию ко всем JS/JSX файлам
    languageOptions: {
      ecmaVersion: 'latest', // Используем последнюю версию ECMAScript
      sourceType: 'module', // Поддержка ES Modules
      globals: {
        ...globals.browser, // Глобальные переменные браузера
        ...globals.node, // Добавляем глобальные переменные Node.js (например, module, require)
      },
      parserOptions: {
        ecmaVersion: 'latest', // Версия ECMAScript для парсера
        ecmaFeatures: { jsx: true }, // Включаем поддержку JSX
        sourceType: 'module', // Тип модуля: ES Module
      },
    },
    plugins: {
      'react-hooks': reactHooks, // Плагин для React Hooks
      'react-refresh': reactRefresh, // Плагин для React Refresh
    },
    rules: {
      // Основные правила ESLint
      ...js.configs.recommended.rules, // Рекомендуемые правила ESLint
      ...reactHooks.configs.recommended.rules, // Рекомендуемые правила для React Hooks

      // Дополнительные правила
      'no-unused-vars': [
        'error',
        { varsIgnorePattern: '^[A-Z_]' }, // Игнорируем переменные, начинающиеся с заглавной буквы или _
      ],
      'react-refresh/only-export-components': [
        'warn',
        { allowConstantExport: true }, // Разрешаем экспорт компонентов как константы
      ],
      'no-undef': 'error', // Запрещаем использование неопределённых переменных
      'no-console': 'warn', // Предупреждение при использовании console
      'react-hooks/rules-of-hooks': 'error', // Проверяем соблюдение правил React Hooks
      'react-hooks/exhaustive-deps': 'warn', // Проверяем зависимости в useEffect и других хуках
    },
  },
];
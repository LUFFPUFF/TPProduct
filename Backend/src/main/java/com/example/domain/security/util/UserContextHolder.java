package com.example.domain.security.util;


import com.example.domain.security.model.UserContext;

public class UserContextHolder {

    private static final ThreadLocal<UserContext> userContextHolder = new ThreadLocal<>();

    private UserContextHolder() {}

    /**
     * Возвращает UserContext для текущего потока.
     * @return UserContext или null, если не установлен
     */
    public static UserContext getContext() {
        return userContextHolder.get();
    }

    /**
     * Устанавливает UserContext для текущего потока.
     * @param userContext контекст пользователя
     */
    public static void setContext(UserContext userContext) {
        userContextHolder.set(userContext);
    }

    /**
     * Очищает UserContext для текущего потока. Важно вызывать после обработки запроса.
     */
    public static void clearContext() {
        userContextHolder.remove();
    }

    /**
     * Возвращает UserContext для текущего потока, выбрасывая исключение, если контекст отсутствует.
     * Полезно для мест, где контекст гарантированно должен быть установлен (например, в AOP аспекте).
     * @return UserContext
     * @throws IllegalStateException если контекст не установлен
     */
    public static UserContext getRequiredContext() {
        UserContext context = getContext();
        if (context == null) {
            throw new IllegalStateException("UserContext is not set for this thread.");
        }
        return context;
    }
}

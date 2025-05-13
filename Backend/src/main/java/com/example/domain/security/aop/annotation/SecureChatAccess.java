package com.example.domain.security.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SecureChatAccess {

    /**
     * Имя параметра метода, который содержит ID сущности (Chat/Message)
     * или объект (DTO), из которого можно получить ID.
     */
    String idParamName();

    /**
     * Имя метода для вызова на объекте (DTO), указанном в {@code idParamName},
     * чтобы получить ID сущности. Используется, если {@code idParamName} указывает на объект, а не сам ID.
     * Например, "getChatId" для DTO с методом getChatId().
     * Если не указано, предполагается, что параметр {@code idParamName} сам является Integer ID.
     */
    String idMethodName() default ""; // Используем пустую строку как маркер "не указано"

    /**
     * Сообщение об ошибке при отсутствии прав доступа.
     */
    String message() default "Access Denied: Insufficient permissions to access this chat or message.";
}

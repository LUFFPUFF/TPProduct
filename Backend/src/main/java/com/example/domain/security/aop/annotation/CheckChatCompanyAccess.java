package com.example.domain.security.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CheckChatCompanyAccess {

    /**
     * Имя параметра метода, который содержит ID чата (Integer chatId)
     * или ID сообщения (Integer messageId).
     * Аспект будет использовать это имя для получения значения ID.
     */
    String idParamName();

    /**
     * Сообщение об ошибке при отсутствии прав доступа к чату/сообщению.
     */
    String message() default "Access Denied: Chat/Message does not belong to your company.";

}

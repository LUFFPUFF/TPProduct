package com.example.domain.security.aop.annotation;

import com.example.database.model.company_subscription_module.user_roles.user.Role;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequireRole {

    /**
     * Список ролей, которым разрешен доступ к помеченному методу.
     * Доступ будет разрешен, если роль пользователя совпадает хотя бы с одной из указанных ролей.
     */
    Role[] allowedRoles();

    /**
     * Сообщение об ошибке при отсутствии прав.
     * @return сообщение
     */
    String message() default "Access Denied: Insufficient role.";
}

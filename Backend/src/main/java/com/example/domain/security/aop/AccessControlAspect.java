package com.example.domain.security.aop;

import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.domain.security.aop.annotation.RequireRole;
import com.example.domain.security.model.UserContext;
import com.example.domain.security.util.UserContextHolder;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.nio.file.AccessDeniedException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
public class AccessControlAspect {

    /**
     * Определяет точку среза для всех методов, помеченных аннотацией @RequireRole.
     */
    @Before("@annotation(requireRole)")
    public void checkRole(JoinPoint joinPoint, RequireRole requireRole) throws AccessDeniedException {
        UserContext userContext = UserContextHolder.getRequiredContext();

        Set<Role> allowedRoles = Arrays.stream(requireRole.allowedRoles()).collect(Collectors.toSet());

        Set<Role> userRoles = userContext.getRoles();

        if (!allowedRoles.contains(userRoles.iterator().next())) {
            String message = requireRole.message() + " Required roles: " + allowedRoles + ", User role: " + userRoles;
            throw new AccessDeniedException(message);
        }

    }
}

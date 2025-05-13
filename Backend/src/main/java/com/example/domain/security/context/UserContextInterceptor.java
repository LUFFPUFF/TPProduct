package com.example.domain.security.context;

import com.example.domain.security.model.UserContext;
import com.example.domain.security.service.UserContextLoader;
import com.example.domain.security.util.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class UserContextInterceptor implements HandlerInterceptor {

    private final UserContextLoader userContextLoader;

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        try {
            UserContext userContext = userContextLoader.loadUserContext();
            UserContextHolder.setContext(userContext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) {
        UserContextHolder.clearContext();
    }
}

package com.example.domain.api.authentication_module.security.filter;

import com.example.domain.api.subscription_module.service.SubscriptionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SubscriptionCheckFilter extends OncePerRequestFilter {
    private final SubscriptionService subscriptionService;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if(request.getRequestURI().contains("/company")
        || request.getRequestURI().contains("/crm")
        || request.getRequestURI().contains("/ui")){
            if (subscriptionService.getSubscription().getEndSubscription().isBefore(LocalDateTime.now())) {

                response.sendRedirect("/subscription");
                // throw new RuntimeException("Срок действия подписки истек, возобновите подписку чтобы продолжить работу");
            }
        }
        filterChain.doFilter(request, response);
    }
}

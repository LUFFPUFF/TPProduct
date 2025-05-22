package com.example.domain.api.authentication_module.security.filter;

import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.domain.api.authentication_module.service.interfaces.CurrentUserDataService;
import com.example.domain.api.subscription_module.service.SubscriptionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SubscriptionCheckFilter extends OncePerRequestFilter {
    private final SubscriptionService subscriptionService;
    private final CurrentUserDataService currentUserDataService;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            filterChain.doFilter(request, response);
            return;
        }
        if(currentUserDataService.hasRole(Role.OPERATOR) || currentUserDataService.hasRole(Role.MANAGER)) {
            if (request.getRequestURI().contains("/company")
                    || request.getRequestURI().contains("/crm")
                    || request.getRequestURI().contains("/ui")) {
                if (subscriptionService.getSubscription().getEndSubscription().isBefore(LocalDateTime.now())) {

                    response.sendRedirect("/subscription");
                    // throw new RuntimeException("Срок действия подписки истек, возобновите подписку чтобы продолжить работу");
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}

package com.example.domain.api.chat_service_api.security;

import com.example.domain.api.authentication_module.security.jwtUtils.JWTUtilsService;
import com.example.domain.api.chat_service_api.config.WebSocketProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthChannelInterceptorAdapter implements ChannelInterceptor {

    private final JWTUtilsService jwtUtilsService;
    private final WebSocketProperties webSocketProperties;

    @Override
    @SuppressWarnings("unchecked")
    public Message<?> preSend(@NotNull Message<?> message, @NotNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        Objects.requireNonNull(accessor, "STOMP header accessor cannot be null");

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String jwtHeaderName = webSocketProperties.getSecurity().getJwt().getHeaderName();
            String jwtTokenPrefix = webSocketProperties.getSecurity().getJwt().getTokenPrefix();

            MultiValueMap<String, String> nativeHeaders =
                    (MultiValueMap<String, String>) accessor.getHeader(SimpMessageHeaderAccessor.NATIVE_HEADERS);

            if (nativeHeaders == null) {
                log.warn("WebSocket CONNECT failed: No native headers found.");
                return message;
            }

            List<String> authorizationHeaders = nativeHeaders.get(jwtHeaderName);

            if (CollectionUtils.isEmpty(authorizationHeaders)) {
                log.warn("WebSocket CONNECT failed: Missing {} header", jwtHeaderName);
                return message;
            }

            String bearerToken = authorizationHeaders.get(0);
            String authToken;

            if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(jwtTokenPrefix)) {
                authToken = bearerToken.substring(jwtTokenPrefix.length());
            } else {
                log.warn("WebSocket CONNECT failed: {} header does not start with {} or is empty.", jwtHeaderName, jwtTokenPrefix);
                return message;
            }

            if (StringUtils.hasText(authToken)) {
                try {
                    if (jwtUtilsService.isTokenExpired(authToken)) {
                        log.warn("WebSocket CONNECT failed: JWT token is expired.");
                        return message;
                    }

                    Claims claims = jwtUtilsService.parseToken(authToken);

                    String email = claims.get("email", String.class);
                    List<String> roles = claims.get("roles", List.class);

                    if (email == null || roles == null) {
                        log.warn("WebSocket CONNECT failed: Email or roles missing in JWT claims.");
                        return message;
                    }

                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    Authentication authentication = new UsernamePasswordAuthenticationToken(email, null, authorities);

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    accessor.setUser(authentication);
                    log.info("WebSocket user '{}' connected and authenticated with roles {}.", email, roles);

                } catch (ExpiredJwtException e) {
                    String userIdentifier = "unknown";
                    try { userIdentifier = jwtUtilsService.getEmail(authToken); } catch (Exception ignored) {}
                    log.warn("WebSocket CONNECT failed: JWT token is expired (caught explicitly). User: {}", userIdentifier, e);
                    return message;
                } catch (Exception e) {
                    log.error("WebSocket CONNECT authentication error: {}", e.getMessage(), e);
                    return message;
                }
            } else {
                log.warn("WebSocket CONNECT failed: Auth token is blank after stripping prefix.");
                return message;
            }
        } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                log.info("WebSocket user '{}' disconnected.", authentication.getName());
            } else if (accessor.getUser() != null) {
                log.info("WebSocket user '{}' disconnected (from accessor).", accessor.getUser().getName());
            } else {
                log.info("WebSocket client disconnected (unauthenticated or session already cleared).");
            }
        }
        return message;
    }
}

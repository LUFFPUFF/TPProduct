package com.example.domain.api.chat_service_api.config;

import com.example.domain.api.chat_service_api.security.AuthChannelInterceptorAdapter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Order;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE + 99)
@EnableConfigurationProperties(WebSocketProperties.class)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AuthChannelInterceptorAdapter authChannelInterceptorAdapter;
    private final WebSocketProperties wsProps;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(wsProps.getEndpoint())
                .setAllowedOriginPatterns(wsProps.getAllowedOrigins().toArray(new String[0]));
                //.withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.setApplicationDestinationPrefixes(wsProps.getStomp().getApplicationDestinationPrefix());
        config.setUserDestinationPrefix(wsProps.getStomp().getUserDestinationPrefix());

        WebSocketProperties.StompProperties.SimpleBrokerProperties simpleBrokerProps = wsProps.getStomp().getSimpleBroker();
        if (simpleBrokerProps.isEnabled()) {
            config.enableSimpleBroker(simpleBrokerProps.getDestinationPrefixes().toArray(new String[0]))
                    .setHeartbeatValue(new long[]{wsProps.getHeartbeat().getInterval(), wsProps.getHeartbeat().getInterval()})
                    .setTaskScheduler(heartBeatScheduler());
        }
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptorAdapter);
        registration.taskExecutor()
                .corePoolSize(wsProps.getBroker().getInboundChannelCorePoolSize())
                .maxPoolSize(wsProps.getBroker().getInboundChannelMaxPoolSize())
                .queueCapacity(Integer.MAX_VALUE)
                .keepAliveSeconds(60);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.taskExecutor()
                .corePoolSize(wsProps.getBroker().getOutboundChannelCorePoolSize())
                .maxPoolSize(wsProps.getBroker().getOutboundChannelMaxPoolSize())
                .queueCapacity(Integer.MAX_VALUE)
                .keepAliveSeconds(60);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(wsProps.getTransportMessageSizeLimit());
        registration.setSendTimeLimit((int) wsProps.getMessage().getSendTimeout());
        registration.setSendBufferSizeLimit(wsProps.getMessage().getBufferSize());
    }

    @Bean
    public ThreadPoolTaskScheduler heartBeatScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(wsProps.getBroker().getTaskSchedulerPoolSize());
        taskScheduler.setThreadNamePrefix("wss-heartbeat-scheduler-");
        taskScheduler.initialize();
        return taskScheduler;
    }
}

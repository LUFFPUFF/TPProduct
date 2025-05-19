package com.example.domain.api.chat_service_api.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@ConfigurationProperties(prefix = "websocket")
@Data
public class WebSocketProperties {

    @NotBlank
    private String endpoint = "/ws";

    @NotEmpty
    private List<String> allowedOrigins= List.of("*");

    @NotNull
    @Valid
    @NestedConfigurationProperty
    private HeartbeatProperties heartbeat = new HeartbeatProperties();

    @NotNull
    @Valid
    @NestedConfigurationProperty
    private MessageProperties message = new MessageProperties();

    @NotNull
    @Valid
    @NestedConfigurationProperty
    private StompProperties stomp = new StompProperties();

    @NotNull
    @Valid
    @NestedConfigurationProperty
    private BrokerThreadPoolProperties broker = new BrokerThreadPoolProperties();

    @NotNull
    @Valid
    @NestedConfigurationProperty
    private SecurityProperties security = new SecurityProperties();

    private int messageSizeLimit = 65536;

    @Data
    @Validated
    public static class HeartbeatProperties {
        @Positive
        private long interval = 25000;
        @Positive
        private long timeout = 5000;
    }

    @Data
    @Validated
    public static class MessageProperties {
        @Positive
        private int bufferSize = 65536;
        @Positive
        private long sendTimeout = 20000;
    }

    @Positive
    private int transportMessageSizeLimit = 131072;

    @Data
    @Validated
    public static class StompProperties {
        @NotNull
        @Valid
        private SimpleBrokerProperties simpleBroker = new SimpleBrokerProperties();

        @NotBlank
        private String applicationDestinationPrefix = "/app";
        @NotBlank
        private String userDestinationPrefix = "/user";

        @Data
        @Validated
        public static class SimpleBrokerProperties {
            private boolean enabled = true;
            @NotEmpty
            private List<String> destinationPrefixes = List.of("/topic/", "/queue/");
        }
    }

    @Data
    @Validated
    public static class BrokerThreadPoolProperties {
        @Positive
        private int taskSchedulerPoolSize = 1;
        @Positive
        private int inboundChannelCorePoolSize = 2;
        @Positive
        private int inboundChannelMaxPoolSize = 4;
        @Positive
        private int outboundChannelCorePoolSize = 2;
        @Positive
        private int outboundChannelMaxPoolSize = 4;
    }

    @Data
    public static class SecurityProperties {
        private JwtProperties jwt = new JwtProperties();

        @Data
        public static class JwtProperties {
            private String headerName = "Authorization";
            private String tokenPrefix = "Bearer ";
        }
    }
}

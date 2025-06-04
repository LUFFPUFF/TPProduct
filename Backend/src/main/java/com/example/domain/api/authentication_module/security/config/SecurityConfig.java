package com.example.domain.api.authentication_module.security.config;

import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.domain.api.authentication_module.security.filter.SubscriptionCheckFilter;
import com.example.domain.api.authentication_module.security.jwtUtils.AuthCookieService;
import com.example.domain.api.authentication_module.security.jwtUtils.JwtRequestFilter;
import com.example.domain.api.chat_service_api.config.WebSocketProperties;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Order;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

import java.util.Arrays;
import java.util.List;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig  {
    private final JwtRequestFilter jwtRequestFilter;
    private final AuthCookieService authCookieService;
    private final SubscriptionCheckFilter subscriptionCheckFilter;
    private final WebSocketProperties wsProps;
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain webSocketSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/ws/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().permitAll()
                )
                .csrf(CsrfConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .anonymous(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(CsrfConfigurer::disable)
                .anonymous(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/auth/**","/api/registration/**").permitAll()
                        .requestMatchers("/api/ui/integration/**").authenticated()
                        .requestMatchers("/api/ui/predefined-answers/download-template").permitAll()
                        .requestMatchers("/api/answers/**").permitAll()
                        .requestMatchers("/api/statistics/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/ui/chats/**").permitAll()
                        .requestMatchers("/api/subscription/extend","/api/company/add").authenticated()
                        .requestMatchers("/test/operator-only").hasAuthority(Role.OPERATOR.getAuthority())
                        .requestMatchers("/api/ui/**","/api/company/get").authenticated()
                        .requestMatchers("/test/no-perm").denyAll()
                        .requestMatchers("/test/auth-only").authenticated()
                        .requestMatchers("/api/company/admin/**").hasAuthority(Role.MANAGER.getAuthority())
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exp -> exp
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            System.out.println("Auth error:\n" +
                                    "" + authException.getMessage()+"\n"+
                                    "--------------------------");
                            response.getWriter().write("{\"error\": \"Unauthorized SCF\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {

                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Access Denied SCF\"}");
                        })
                )
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(subscriptionCheckFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(logout -> logout.logoutSuccessUrl("/all-perm"));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(wsProps.getAllowedOrigins());
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}

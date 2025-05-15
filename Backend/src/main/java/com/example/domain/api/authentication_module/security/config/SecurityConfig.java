package com.example.domain.api.authentication_module.security.config;

import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.domain.api.authentication_module.security.filter.SubscriptionCheckFilter;
import com.example.domain.api.authentication_module.security.jwtUtils.AuthCookieService;
import com.example.domain.api.authentication_module.security.jwtUtils.JwtRequestFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig  {
    private final JwtRequestFilter jwtRequestFilter;
    private final AuthCookieService authCookieService;
    private final SubscriptionCheckFilter subscriptionCheckFilter;
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(CsrfConfigurer::disable)
                .anonymous(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth","/api/registration").permitAll()
                        .requestMatchers("/api/subscription/extend","/api/company/add").hasAuthority(Role.MANAGER.getAuthority())
                        .requestMatchers("/test/operator-only").hasAuthority(Role.OPERATOR.getAuthority())
                        .requestMatchers("/api/ui/","/api/company/get").hasAnyAuthority(Role.OPERATOR.getAuthority(), Role.MANAGER.getAuthority())
                        .requestMatchers("/test/no-perm").denyAll()
                        .requestMatchers("/test/auth-only").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exp -> exp
                        .authenticationEntryPoint(
                                (request, response, authException) -> response.sendRedirect("/login")
                        )
                        .accessDeniedHandler((request, response, accessDeniedException) ->{
                            authCookieService.ExpireTokenCookie(response);
                            response.sendRedirect("https://dialogx.ru/login");
                        }))
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(subscriptionCheckFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(withDefaults())
                .logout(logout -> logout.logoutSuccessUrl("/all-perm"));

        return http.build();
    }

}

package com.example.domain.api.authentication_service.security.config;

import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.domain.api.authentication_service.security.jwtUtils.JwtRequestFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
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
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(CsrfConfigurer::disable)
        .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/test/all-perm").permitAll()
                        .requestMatchers("/test/manager-only").hasAuthority(Role.MANAGER.getAuthority())
                        .requestMatchers("/test/operator-only").hasAuthority(Role.OPERATOR.getAuthority())
                        .requestMatchers("/test/company-data").hasAnyAuthority(Role.OPERATOR.getAuthority(), Role.MANAGER.getAuthority())
                        .requestMatchers("/test/no-perm").denyAll()
                        .requestMatchers("/test/auth-only").authenticated()
                        .anyRequest().permitAll()

                )
                .exceptionHandling(exp -> exp
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(withDefaults())
                .logout(logout -> logout.logoutSuccessUrl("/all-perm"));

        return http.build();
    }

}

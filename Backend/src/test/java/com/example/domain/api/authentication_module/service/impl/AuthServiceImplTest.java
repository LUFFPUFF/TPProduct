package com.example.domain.api.authentication_module.service.impl;

import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.authentication_module.cache.AuthCacheService;
import com.example.domain.api.authentication_module.exception_handler_auth.NotFoundUserException;
import com.example.domain.api.authentication_module.exception_handler_auth.WrongPasswordException;
import com.example.domain.api.authentication_module.security.jwtUtils.JWTUtilsService;
import com.example.domain.api.authentication_module.service.interfaces.AuthService;
import com.example.domain.api.statistics_module.aop.ChatMetricsAspect; // Импортируем аспект
import com.example.domain.dto.TokenDto;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// Указываем классы, которые нужно загрузить в контекст для этого теста
@SpringBootTest(classes = {
        AuthServiceImpl.class,
        ChatMetricsAspect.class,
        AuthServiceImplTest.TestConfig.class
})
class AuthServiceImplTest {

    @Configuration
    // Если лог показывал "AuthServiceImpl.login", значит CGLIB сработал.
    // Это могло произойти, если ты оставил proxyTargetClass = true, или если
    // Spring по какой-то причине решил использовать CGLIB.
    // Оставь @EnableAspectJAutoProxy без явного proxyTargetClass, если ты его убирал.
    // Если лог все еще "AuthService.login", и аспект не находит аннотации, тогда proxyTargetClass=true нужно.
    @EnableAspectJAutoProxy // (попробуй proxyTargetClass = true, если метрики опять 0)
    static class TestConfig {
        @Bean
        public MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        public ApplicationContext applicationContext() {
            return mock(ApplicationContext.class);
        }
    }

    @Autowired
    private AuthService authService; // Инъекция по интерфейсу

    @Autowired
    private MeterRegistry meterRegistry;

    @MockBean
    private JWTUtilsService jwtUtilsService;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private AuthCacheService authCacheService;
    @MockBean
    private UserDetailsService userDetailsService;
    @MockBean
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        meterRegistry.clear();
    }

    private UserDetails createMockUserDetails(String email, String password) {
        // ... (код без изменений)
        return new UserDetails() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() { return Collections.emptyList(); }
            @Override public String getPassword() { return password; }
            @Override public String getUsername() { return email; }
            @Override public boolean isAccountNonExpired() { return true; }
            @Override public boolean isAccountNonLocked() { return true; }
            @Override public boolean isCredentialsNonExpired() { return true; }
            @Override public boolean isEnabled() { return true; }
        };
    }

    @Test
    void login_successful_shouldIncrementLoginSuccessTotalCounter() {
        // Arrange
        String email = "test@example.com";
        String rawPassword = "password";
        String encodedPassword = "encodedPassword";
        User mockUserEntity = new User();
        mockUserEntity.setEmail(email);
        mockUserEntity.setPassword(encodedPassword);
        UserDetails mockUserDetails = createMockUserDetails(email, encodedPassword);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUserEntity));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(mockUserDetails);
        TokenDto mockTokenDto = TokenDto.builder()
                .access_token("access-token")
                .refresh_token("refresh-token")
                .build();
        when(jwtUtilsService.generateTokensByUser(any(UserDetails.class)))
                .thenReturn(mockTokenDto);
        // Act
        authService.login(email, rawPassword);

        // Assert
        assertThat(meterRegistry.get("login_success_total").counter().count()).isEqualTo(1.0);

        // Проверка, что другие счетчики ошибок равны 0 (или не существуют)
        assertCounterValueIsZeroOrNotExists("login_failure_user_not_found_total");
        assertCounterValueIsZeroOrNotExists("login_failure_wrong_password_total");
    }

    @Test
    void login_userNotFound_shouldIncrementLoginFailureUserNotFoundTotalCounter() {
        // Arrange
        String email = "nonexistent@example.com";
        String rawPassword = "password";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundUserException.class, () -> {
            authService.login(email, rawPassword);
        });

        assertThat(meterRegistry.get("login_failure_user_not_found_total").counter().count()).isEqualTo(1.0);
        assertCounterValueIsZeroOrNotExists("login_success_total");
        assertCounterValueIsZeroOrNotExists("login_failure_wrong_password_total");
    }

    @Test
    void login_wrongPassword_shouldIncrementLoginFailureWrongPasswordTotalCounter() {
        // Arrange
        String email = "test@example.com";
        String rawPassword = "wrong_password";
        String encodedPassword = "encodedPassword";
        User mockUserEntity = new User();
        mockUserEntity.setEmail(email);
        mockUserEntity.setPassword(encodedPassword);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUserEntity));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(false);

        // Act & Assert
        assertThrows(WrongPasswordException.class, () -> {
            authService.login(email, rawPassword);
        });

        assertThat(meterRegistry.get("login_failure_wrong_password_total").counter().count()).isEqualTo(1.0);
        assertCounterValueIsZeroOrNotExists("login_success_total");
        assertCounterValueIsZeroOrNotExists("login_failure_user_not_found_total");
    }

    @Test
    void logout_successful_shouldIncrementLogoutSuccessTotalCounter() {
        // Arrange
        String refreshToken = "some-refresh-token";

        // Act
        authService.logout(refreshToken);

        // Assert
        assertThat(meterRegistry.get("logout_success_total").counter().count()).isEqualTo(1.0);
    }

    // Вспомогательный метод для проверки, что счетчик равен 0 или не существует
    private void assertCounterValueIsZeroOrNotExists(String metricName) {
        io.micrometer.core.instrument.Counter counter = meterRegistry.find(metricName).counter();
        if (counter != null) {
            assertThat(counter.count()).isZero();
        } else {
            // Если счетчик не найден, это тоже означает 0 инкрементов, что для нас ОК
            assertThat(true).isTrue();
        }
    }
}
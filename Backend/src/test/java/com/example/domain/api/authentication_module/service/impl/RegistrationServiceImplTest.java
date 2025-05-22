package com.example.domain.api.authentication_module.service.impl;

import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.authentication_module.cache.AuthCacheService;
import com.example.domain.api.authentication_module.exception_handler_auth.EmailExistsException;
import com.example.domain.api.authentication_module.security.jwtUtils.JWTUtilsService;
import com.example.domain.api.authentication_module.service.interfaces.RegistrationService;
import com.example.domain.api.authentication_module.service.interfaces.RoleService;
import com.example.domain.api.statistics_module.aop.MetricsAspect;
import com.example.domain.dto.RegistrationDto;
import com.example.domain.dto.TokenDto;
import com.example.domain.dto.mapper.MapperDto;
import io.micrometer.core.instrument.MeterRegistry;
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

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {
        RegistrationServiceImpl.class,
        MetricsAspect.class,
        RegistrationServiceImplTest.TestConfig.class
})
class RegistrationServiceImplTest {

    @Configuration
    // Оставляем без proxyTargetClass=true, чтобы симулировать проблему с JDK-прокси,
    // если бэкендер не внес изменения. Если он разрешит proxyTargetClass=true в тестах,
    // или внесет изменения в основной код, то аспект должен будет находить аннотации.
    @EnableAspectJAutoProxy
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
    private RegistrationService registrationService; // Инъекция по интерфейсу

    @Autowired
    private MeterRegistry meterRegistry;

    @MockBean
    private UserRepository userRepository;
    @MockBean
    private MapperDto mapperDto;
    @MockBean
    private JWTUtilsService jwtUtilsService;
    @MockBean
    private PasswordEncoder passwordEncoder;
    @MockBean
    private UserDetailsService userDetailsService;
    @MockBean
    private AuthCacheService authCacheService;
    @MockBean
    private RoleService roleService;

    @BeforeEach
    void setUp() {
        meterRegistry.clear();
    }

    private UserDetails createMockUserDetails(String email, String password) {
        return new UserDetails() {
            @Override public Collection<? extends GrantedAuthority> getAuthorities() { return Collections.emptyList(); }
            @Override public String getPassword() { return password; }
            @Override public String getUsername() { return email; }
            @Override public boolean isAccountNonExpired() { return true; }
            @Override public boolean isAccountNonLocked() { return true; }
            @Override public boolean isCredentialsNonExpired() { return true; }
            @Override public boolean isEnabled() { return true; }
        };
    }

    @Test
    void registerUser_newUser_shouldIncrementAttemptAndCodeSentSuccessCounters() {
        // Arrange
        RegistrationDto registrationDto = RegistrationDto.builder()
                .email("newuser@example.com")
                .password("password123")
                .build();
        String encodedPass = "encodedPassword";

        when(userRepository.findByEmail(registrationDto.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(registrationDto.getPassword())).thenReturn(encodedPass);
        // Метод sendRegistrationCode внутри registerUser должен вызываться,
        // и он вернет true, т.к. authCacheService.putRegistrationCode мокнут не будет кидать исключение
        // и сам метод sendRegistrationCode всегда возвращает true.

        // Act
        Boolean result = registrationService.registerUser(registrationDto);

        // Assert
        assertTrue(result);

        // Проверка метрики attempt_total (сработает, если аспект найдет аннотацию)
        assertMetricCount("attempt_total", 1.0, "registerUser");

        // Проверка метрики code_sent_success_total (сработает, если аспект найдет аннотацию)
        assertMetricCount("code_sent_success_total", 1.0, "sendRegistrationCode");
        assertCounterValueIsZeroOrNotExists("code_sent_failure_total");

        // Проверим, что passwordEncoder.encode был вызван
        verify(passwordEncoder).encode(registrationDto.getPassword());
        // Проверим, что authCacheService.putRegistrationCode был вызван с кодом "000000"
        // и корректным DTO (email, encodedPassword, fullName=email, createdAt, updatedAt)
        RegistrationDto expectedDtoInCache = RegistrationDto.builder()
                .email(registrationDto.getEmail())
                .password(encodedPass) // Важно: уже закодированный пароль
                .fullName(registrationDto.getEmail()) // Логика из registerUser
                .createdAt(registrationDto.getCreatedAt()) // Должен быть установлен в registerUser
                .updatedAt(registrationDto.getUpdatedAt()) // Должен быть установлен в registerUser
                .build();
        // Для createdAt/updatedAt нужно будет либо мокать LocalDateTime.now(), либо использовать ArgumentCaptor
        // Пока что проверим просто вызов с любым RegistrationDto, но email должен совпадать
        verify(authCacheService).putRegistrationCode(eq("000000"), argThat(dto ->
                        dto.getEmail().equals(expectedDtoInCache.getEmail()) &&
                                dto.getPassword().equals(expectedDtoInCache.getPassword()) &&
                                dto.getFullName().equals(expectedDtoInCache.getFullName())
                // Проверка createdAt/updatedAt требует более сложного матчера или мокирования времени
        ));
    }

    @Test
    void registerUser_emailExists_shouldThrowEmailExistsExceptionAndIncrementAttemptCounter() {
        // Arrange
        RegistrationDto registrationDto = RegistrationDto.builder()
                .email("existinguser@example.com")
                .password("password123")
                .build();

        when(userRepository.findByEmail(registrationDto.getEmail())).thenReturn(Optional.of(new User()));

        // Act & Assert
        assertThrows(EmailExistsException.class, () -> {
            registrationService.registerUser(registrationDto);
        });

        // Проверка метрики attempt_total (сработает, если аспект найдет аннотацию)
        assertMetricCount("attempt_total", 1.0, "registerUser (email exists)");

        assertCounterValueIsZeroOrNotExists("code_sent_success_total");
        assertCounterValueIsZeroOrNotExists("code_sent_failure_total");
    }

    @Test
    void checkRegistrationCode_validCode_shouldIncrementSuccessCounterAndAddRole() {
        // Arrange
        String registrationCode = "000000";
        RegistrationDto mockRegistrationDtoInCache = RegistrationDto.builder()
                .email("testuser@example.com")
                .password("encodedPassword")
                .fullName("Test User")
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .updatedAt(LocalDateTime.now().minusMinutes(5))
                .build();

        User newUserEntity = new User();
        newUserEntity.setEmail(mockRegistrationDtoInCache.getEmail());

        UserDetails mockUserDetails = createMockUserDetails(mockRegistrationDtoInCache.getEmail(), "encodedPassword");
        TokenDto expectedTokenDto = TokenDto.builder()
                .access_token("access")
                .refresh_token("refresh")
                .build();
        when(authCacheService.getRegistrationCode(registrationCode)).thenReturn(Optional.of(mockRegistrationDtoInCache));
        when(mapperDto.toEntityUserFromRegistration(mockRegistrationDtoInCache)).thenReturn(newUserEntity);
        when(userRepository.save(any(User.class))).thenReturn(newUserEntity);
        when(roleService.addRole(mockRegistrationDtoInCache.getEmail(), Role.USER)).thenReturn(true);
        when(userDetailsService.loadUserByUsername(mockRegistrationDtoInCache.getEmail())).thenReturn(mockUserDetails);
        when(jwtUtilsService.generateTokensByUser(mockUserDetails)).thenReturn(expectedTokenDto);

        // Act
        TokenDto actualTokenDto = registrationService.checkRegistrationCode(registrationCode);

        // Assert
        assertThat(actualTokenDto).isEqualTo(expectedTokenDto);

        // Проверка метрики code_check_success_total (сработает, если аспект найдет аннотацию)
        assertMetricCount("code_check_success_total", 1.0, "checkRegistrationCode (valid)");
        assertCounterValueIsZeroOrNotExists("code_check_failure_invalid_total");

        verify(userRepository).save(newUserEntity);
        verify(roleService).addRole(mockRegistrationDtoInCache.getEmail(), Role.USER);
    }

    @Test
    void checkRegistrationCode_invalidCode_shouldThrowExceptionAndIncrementFailureCounter() {
        // Arrange
        String invalidCode = "invalid123";
        when(authCacheService.getRegistrationCode(invalidCode)).thenReturn(Optional.empty());

        // Проверка метрики code_check_failure_invalid_total (сработает, если аспект найдет аннотацию и SpEL ПРАВИЛЬНЫЙ)
        // Убедись, что SpEL в RegistrationServiceImpl:
        // conditionSpEL = "#throwable instanceof T(com.example.domain.api.authentication_module.exception_handler_auth.InvalidRegistrationCodeException)"
        assertMetricCount("code_check_failure_invalid_total", 1.0, "checkRegistrationCode (invalid)");
        assertCounterValueIsZeroOrNotExists("code_check_success_total");
    }

    // Вспомогательные методы для проверки счетчиков
    private void assertMetricCount(String metricName, double expectedCount, String testCaseDescription) {
        try {
            double actualCount = meterRegistry.get(metricName).counter().count();
            assertThat(actualCount)
                    .withFailMessage("For metric '%s' in test case '%s': expected count <%s> but was <%s>",
                            metricName, testCaseDescription, expectedCount, actualCount)
                    .isEqualTo(expectedCount);
        } catch (io.micrometer.core.instrument.search.MeterNotFoundException e) {
            if (expectedCount > 0) {
                throw new AssertionError("For metric '" + metricName + "' in test case '" + testCaseDescription +
                        "': metric not found, but expected count " + expectedCount, e);
            }
            // Если ожидали 0 и не нашли - это не всегда ошибка, зависит от логики.
            // Для простоты, если ожидаем >0, а метрики нет - это провал.
        }
    }

    private void assertCounterValueIsZeroOrNotExists(String metricName) {
        io.micrometer.core.instrument.Counter counter = meterRegistry.find(metricName).counter();
        if (counter != null) {
            assertThat(counter.count())
                    .withFailMessage("For metric '%s': expected count <0.0> or not to exist, but was <%s>",
                            metricName, counter.count())
                    .isZero();
        } else {
            assertThat(true).isTrue(); // OK if not found, means 0 increments
        }
    }
}
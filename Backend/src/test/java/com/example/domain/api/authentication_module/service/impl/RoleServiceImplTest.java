package com.example.domain.api.authentication_module.service.impl;

import com.example.database.model.company_subscription_module.user_roles.UserRole;
import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.database.repository.company_subscription_module.UserRoleRepository;
import com.example.domain.api.authentication_module.cache.AuthCacheService;
import com.example.domain.api.authentication_module.exception_handler_auth.NotFoundUserException;
import com.example.domain.api.authentication_module.service.interfaces.RoleService; // Используем интерфейс
import com.example.domain.api.statistics_module.aop.MetricsAspect;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {
        RoleServiceImpl.class,
        MetricsAspect.class,
        RoleServiceImplTest.TestConfig.class
})
class RoleServiceImplTest {

    @Configuration
    @EnableAspectJAutoProxy // Оставляем без proxyTargetClass=true для демонстрации проблемы
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
    private RoleService roleService; // Инъекция по интерфейсу

    @Autowired
    private MeterRegistry meterRegistry;

    @MockBean
    private UserRoleRepository userRoleRepository;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private AuthCacheService authCacheService;

    private User mockUser;
    private String userEmail = "testuser@example.com";

    @BeforeEach
    void setUp() {
        meterRegistry.clear();

        mockUser = new User();
        mockUser.setId(1);
        mockUser.setEmail(userEmail);
        // ... другие необходимые поля User

        // Мокируем UserContextHolder, если он используется косвенно (пока не видно в RoleServiceImpl)
        // com.example.domain.security.util.UserContextHolder.clearContext();
    }

    @Test
    void addRole_userExists_shouldAddRoleAndIncrementAddedSuccessCounter() {
        // Arrange
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(mockUser));
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // Mockito.doNothing().when(authCacheService).putExpiredData(userEmail);

        // Act
        boolean result = roleService.addRole(userEmail, Role.OPERATOR);

        // Assert
        assertTrue(result);
        verify(authCacheService).putExpiredData(userEmail);
        verify(userRoleRepository).save(argThat(ur ->
                ur.getUser().equals(mockUser) && ur.getRole() == Role.OPERATOR
        ));

        // Проверка метрики added_success_total (сработает, если аспект найдет аннотацию)
        // Имя "added_success_total", тег "role_name" со значением "OPERATOR"
        // SpEL для тега: #role.name()
        // SpEL для условия: #result == true
        assertMetricCountWithTags("added_success_total", 1.0, "addRole OPERATOR (success)",
                "role_name", "OPERATOR");
    }

    @Test
    void addRole_userNotFound_shouldThrowNotFoundUserException() {
        // Arrange
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundUserException.class, () -> {
            roleService.addRole(userEmail, Role.OPERATOR);
        });

        // Метрика added_success_total не должна инкрементироваться, так как result не будет true
        // и будет брошено исключение до того, как условие #result == true будет оценено
        assertCounterValueIsZeroOrNotExists("added_success_total");
        // Также нужно проверить, что не было попытки инкрементировать с каким-либо тегом
        io.micrometer.core.instrument.Meter.Id expectedId = new io.micrometer.core.instrument.Meter.Id(
                "added_success_total",
                io.micrometer.core.instrument.Tags.of("role_name", "OPERATOR"),
                null, null, io.micrometer.core.instrument.Meter.Type.COUNTER);
        assertThat(meterRegistry.find(expectedId.getName()).tags(expectedId.getTags()).counter()).isNull();


        verify(authCacheService).putExpiredData(userEmail); // Этот вызов произойдет до userRepository.findByEmail
        verify(userRoleRepository, never()).save(any(UserRole.class));
    }

    @Test
    void removeRole_userExists_shouldRemoveRoleAndIncrementRemovedSuccessCounter() {
        // Arrange
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(mockUser));
        // Mockito.doNothing().when(userRoleRepository).deleteByUserAndRole(mockUser, Role.OPERATOR);
        // Mockito.doNothing().when(authCacheService).putExpiredData(userEmail);

        // Act
        boolean result = roleService.removeRole(userEmail, Role.OPERATOR);

        // Assert
        assertTrue(result);
        verify(authCacheService).putExpiredData(userEmail);
        verify(userRoleRepository).deleteByUserAndRole(mockUser, Role.OPERATOR);

        // Проверка метрики removed_success_total (сработает, если аспект найдет аннотацию)
        // Имя "removed_success_total", тег "role_name" со значением "OPERATOR"
        // SpEL для тега: #role.name()
        // SpEL для условия: #result == true
        assertMetricCountWithTags("removed_success_total", 1.0, "removeRole OPERATOR (success)",
                "role_name", "OPERATOR");
    }

    @Test
    void removeRole_userNotFound_shouldThrowNotFoundUserException() {
        // Arrange
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundUserException.class, () -> {
            roleService.removeRole(userEmail, Role.OPERATOR);
        });

        // Метрика removed_success_total не должна инкрементироваться
        assertCounterValueIsZeroOrNotExists("removed_success_total");
        io.micrometer.core.instrument.Meter.Id expectedId = new io.micrometer.core.instrument.Meter.Id(
                "removed_success_total",
                io.micrometer.core.instrument.Tags.of("role_name", "OPERATOR"),
                null, null, io.micrometer.core.instrument.Meter.Type.COUNTER);
        assertThat(meterRegistry.find(expectedId.getName()).tags(expectedId.getTags()).counter()).isNull();

        verify(authCacheService).putExpiredData(userEmail);
        verify(userRoleRepository, never()).deleteByUserAndRole(any(User.class), any(Role.class));
    }


    // Вспомогательные методы для проверки счетчиков
    private void assertMetricCountWithTags(String metricName, double expectedCount, String testCaseDescription, String... tags) {
        try {
            double actualCount = meterRegistry.get(metricName).tags(tags).counter().count();
            assertThat(actualCount)
                    .withFailMessage("For metric '%s' with tags %s in test case '%s': expected count <%s> but was <%s>",
                            metricName, java.util.Arrays.toString(tags), testCaseDescription, expectedCount, actualCount)
                    .isEqualTo(expectedCount);
        } catch (io.micrometer.core.instrument.search.MeterNotFoundException e) {
            if (expectedCount > 0) {
                throw new AssertionError("For metric '" + metricName + "' with tags " + java.util.Arrays.toString(tags) +
                        " in test case '" + testCaseDescription +
                        "': metric not found, but expected count " + expectedCount, e);
            }
        }
    }

    private void assertCounterValueIsZeroOrNotExists(String metricName) {
        // Эта проверка более общая, так как теги могут меняться
        // Если нам нужно проверить отсутствие метрики с конкретными тегами,
        // то нужно использовать meterRegistry.find(metricName).tags(...).counter()
        meterRegistry.forEachMeter(meter -> {
            if (meter.getId().getName().equals(metricName)) {
                if (meter.getId().getType() == io.micrometer.core.instrument.Meter.Type.COUNTER) {
                    assertThat(((io.micrometer.core.instrument.Counter) meter).count())
                            .withFailMessage("For metric '%s': expected count <0.0>, but was <%s>",
                                    metricName, ((io.micrometer.core.instrument.Counter) meter).count())
                            .isZero();
                }
            }
        });
        // Если метрика вообще не найдена, это тоже 0 инкрементов
        if (meterRegistry.find(metricName).meter() == null) {
            assertThat(true).isTrue();
        }
    }
}
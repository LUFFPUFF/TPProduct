package com.example.domain.api.authentication_module.service.impl;

import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.authentication_module.cache.AuthCacheService;
import com.example.domain.api.authentication_module.exception_handler_auth.NotFoundUserException;
import com.example.domain.api.authentication_module.security.jwtUtils.JWTUtilsService;
// Убираем интерфейс AuthService из импортов, т.к. тестируем реализацию
// import com.example.domain.api.authentication_module.service.interfaces.AuthService;
import com.example.domain.dto.TokenDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;


import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // Для удобства с @BeforeEach
class AuthServiceImplTest {

    @Mock private JWTUtilsService jwtUtilsService;
    @Mock private UserRepository userRepository;
    @Mock private AuthCacheService authCacheService;
    @Mock private UserDetailsService userDetailsService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testDbUser;
    private UserDetails testUserDetails;
    private TokenDto testTokenDto;
    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_PASSWORD = "password123";
    private final String ENCODED_PASSWORD = "encodedPassword123";
    private final String REFRESH_TOKEN = "sampleRefreshToken";
    private final String ACCESS_TOKEN = "sampleAccessToken";


    @BeforeEach
    void setUp() {
        reset(jwtUtilsService, userRepository, authCacheService, userDetailsService, passwordEncoder);

        testDbUser = new User();
        testDbUser.setId(1);
        testDbUser.setEmail(TEST_EMAIL);
        testDbUser.setPassword(ENCODED_PASSWORD);

        // Мок UserDetails (обычно это org.springframework.security.core.userdetails.User)
        testUserDetails = mock(UserDetails.class); // Можем мокнуть или создать реальный User
        when(testUserDetails.getUsername()).thenReturn(TEST_EMAIL);
        // when(testUserDetails.getPassword()).thenReturn(ENCODED_PASSWORD); // Не обязательно для этого теста

        testTokenDto = TokenDto.builder()
                .access_token(ACCESS_TOKEN)
                .refresh_token(REFRESH_TOKEN)
                .build();
    }

    // --- Тесты для login ---

    @Test
    void login_Successful_ShouldReturnTokenDto() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testDbUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(userDetailsService.loadUserByUsername(TEST_EMAIL)).thenReturn(testUserDetails);
        when(jwtUtilsService.generateTokensByUser(testUserDetails)).thenReturn(testTokenDto);

        // Act
        TokenDto result = authService.login(TEST_EMAIL, TEST_PASSWORD);

        // Assert
        assertNotNull(result);
        assertEquals(ACCESS_TOKEN, result.getAccess_token());
        assertEquals(REFRESH_TOKEN, result.getRefresh_token());

        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(passwordEncoder).matches(TEST_PASSWORD, ENCODED_PASSWORD);
        verify(userDetailsService).loadUserByUsername(TEST_EMAIL);
        verify(jwtUtilsService).generateTokensByUser(testUserDetails);
    }

    @Test
    void login_UserNotFound_ShouldThrowNotFoundUserException() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundUserException.class, () -> {
            authService.login(TEST_EMAIL, TEST_PASSWORD);
        });

        verify(userRepository).findByEmail(TEST_EMAIL);
        verifyNoInteractions(passwordEncoder, userDetailsService, jwtUtilsService);
    }

    @Test
    void login_IncorrectPassword_ShouldThrowNotFoundUserException() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testDbUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(false); // Пароль не совпадает

        // Act & Assert
        assertThrows(NotFoundUserException.class, () -> {
            authService.login(TEST_EMAIL, TEST_PASSWORD);
        });

        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(passwordEncoder).matches(TEST_PASSWORD, ENCODED_PASSWORD);
        verifyNoInteractions(userDetailsService, jwtUtilsService);
    }

    @Test
    void login_UserDetailsServiceThrowsException_ShouldThrowException() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testDbUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        UsernameNotFoundException serviceException = new UsernameNotFoundException("User details service error");
        when(userDetailsService.loadUserByUsername(TEST_EMAIL)).thenThrow(serviceException);

        // Act & Assert
        UsernameNotFoundException thrown = assertThrows(UsernameNotFoundException.class, () -> {
            authService.login(TEST_EMAIL, TEST_PASSWORD);
        });
        assertEquals(serviceException, thrown); // Ожидаем проброс исходного исключения

        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(passwordEncoder).matches(TEST_PASSWORD, ENCODED_PASSWORD);
        verify(userDetailsService).loadUserByUsername(TEST_EMAIL);
        verifyNoInteractions(jwtUtilsService);
    }

    @Test
    void login_JwtUtilsServiceThrowsException_ShouldThrowException() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testDbUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(userDetailsService.loadUserByUsername(TEST_EMAIL)).thenReturn(testUserDetails);
        RuntimeException jwtException = new RuntimeException("Token generation failed");
        when(jwtUtilsService.generateTokensByUser(testUserDetails)).thenThrow(jwtException);

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            authService.login(TEST_EMAIL, TEST_PASSWORD);
        });
        assertEquals(jwtException, thrown);

        verify(jwtUtilsService).generateTokensByUser(testUserDetails);
    }

    // --- Тесты для logout ---

    @Test
    void logout_Successful_ShouldCallCacheAndReturnTrue() {
        // Arrange
        doNothing().when(authCacheService).removeRefreshToken(REFRESH_TOKEN);

        // Act
        boolean result = authService.logout(REFRESH_TOKEN);

        // Assert
        assertTrue(result);
        verify(authCacheService).removeRefreshToken(REFRESH_TOKEN);
    }

    @Test
    void logout_AuthCacheServiceThrowsException_ShouldThrowException() {
        // Arrange
        RuntimeException cacheException = new RuntimeException("Cache removal failed");
        doThrow(cacheException).when(authCacheService).removeRefreshToken(REFRESH_TOKEN);

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            authService.logout(REFRESH_TOKEN);
        });
        assertEquals(cacheException, thrown);

        verify(authCacheService).removeRefreshToken(REFRESH_TOKEN);
    }
}
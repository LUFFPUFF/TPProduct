package com.example.domain.api.authentication_module.security.jwtUtils.impl;

import com.example.domain.dto.TokenDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock; // Не нужен, т.к. нет зависимостей для мокирования
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;


import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // Не строго, т.к. мокируем Servlet API
class AuthCookieServiceImplTest {

    // Зависимостей для @Mock нет, так как сервис простой

    @InjectMocks // Используем @InjectMocks, чтобы создать экземпляр AuthCookieServiceImpl
    private AuthCookieServiceImpl authCookieService;

    @Mock
    private HttpServletRequest mockRequest; // Мок для запроса

    @Mock
    private HttpServletResponse mockResponse; // Мок для ответа

    @Captor
    private ArgumentCaptor<Cookie> cookieCaptor; // Каптор для проверки куки

    private final String ACCESS_TOKEN_VALUE = "testAccessTokenValue";
    private final String REFRESH_TOKEN_VALUE = "testRefreshTokenValue";
    private final String ACCESS_TOKEN_NAME = "access_token";
    private final String REFRESH_TOKEN_NAME = "refresh_token";

    @BeforeEach
    void setUp() {
        reset(mockRequest, mockResponse); // Сброс моков перед каждым тестом
    }

    // --- Тесты для setTokenCookies ---

    @Test
    void setTokenCookies_ValidTokens_ShouldAddTwoCookiesWithCorrectAttributes() {
        // Arrange
        TokenDto tokenDto = TokenDto.builder()
                .access_token(ACCESS_TOKEN_VALUE)
                .refresh_token(REFRESH_TOKEN_VALUE)
                .build();
        doNothing().when(mockResponse).addCookie(any(Cookie.class));

        // Act
        authCookieService.setTokenCookies(mockResponse, tokenDto);

        // Assert
        verify(mockResponse, times(2)).addCookie(cookieCaptor.capture());
        List<Cookie> capturedCookies = cookieCaptor.getAllValues();

        Cookie accessTokenCookie = capturedCookies.stream()
                .filter(c -> ACCESS_TOKEN_NAME.equals(c.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Access token cookie not found"));

        Cookie refreshTokenCookie = capturedCookies.stream()
                .filter(c -> REFRESH_TOKEN_NAME.equals(c.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Refresh token cookie not found"));

        // Проверка Access Token Cookie
        assertEquals(ACCESS_TOKEN_VALUE, accessTokenCookie.getValue());
        assertTrue(accessTokenCookie.isHttpOnly());
        assertTrue(accessTokenCookie.getSecure());
        assertEquals("/", accessTokenCookie.getPath());
        assertEquals(60 * 15, accessTokenCookie.getMaxAge());

        // Проверка Refresh Token Cookie
        assertEquals(REFRESH_TOKEN_VALUE, refreshTokenCookie.getValue());
        assertTrue(refreshTokenCookie.isHttpOnly());
        assertTrue(refreshTokenCookie.getSecure());
        assertEquals("/", refreshTokenCookie.getPath());
        assertEquals(60 * 60 * 24 * 3, refreshTokenCookie.getMaxAge());
    }

    @Test
    void setTokenCookies_NullTokenDto_ShouldThrowNullPointerException() {
        // Act & Assert
        // Сервис не проверяет tokenDto на null, поэтому NPE при попытке доступа к tokenDto.getAccess_token()
        assertThrows(NullPointerException.class, () -> {
            authCookieService.setTokenCookies(mockResponse, null);
        });
        verify(mockResponse, never()).addCookie(any());
    }

    @Test
    void setTokenCookies_NullAccessTokenInDto_ShouldCreateCookieWithNullValue() {
        // Arrange
        TokenDto tokenDto = TokenDto.builder()
                .access_token(null) // Access token null
                .refresh_token(REFRESH_TOKEN_VALUE)
                .build();
        // Act
        authCookieService.setTokenCookies(mockResponse, tokenDto);
        // Assert
        verify(mockResponse, times(2)).addCookie(cookieCaptor.capture());
        Cookie accessTokenCookie = cookieCaptor.getAllValues().stream()
                .filter(c -> ACCESS_TOKEN_NAME.equals(c.getName())).findFirst().orElse(null);
        assertNotNull(accessTokenCookie);
        assertNull(accessTokenCookie.getValue(), "Access token cookie value should be null if DTO's access token is null");
    }


    // --- Тесты для getTokensCookie ---

    @Test
    void getTokensCookie_BothCookiesPresent_ShouldReturnTokenDtoWithValues() {
        // Arrange
        Cookie accessCookie = new Cookie(ACCESS_TOKEN_NAME, ACCESS_TOKEN_VALUE);
        Cookie refreshCookie = new Cookie(REFRESH_TOKEN_NAME, REFRESH_TOKEN_VALUE);
        when(mockRequest.getCookies()).thenReturn(new Cookie[]{accessCookie, refreshCookie});

        // Act
        TokenDto result = authCookieService.getTokensCookie(mockRequest);

        // Assert
        assertNotNull(result);
        assertEquals(ACCESS_TOKEN_VALUE, result.getAccess_token());
        assertEquals(REFRESH_TOKEN_VALUE, result.getRefresh_token());
    }

    @Test
    void getTokensCookie_OnlyAccessTokenPresent_ShouldReturnDtoWithAccessTokenOnly() {
        Cookie accessCookie = new Cookie(ACCESS_TOKEN_NAME, ACCESS_TOKEN_VALUE);
        when(mockRequest.getCookies()).thenReturn(new Cookie[]{accessCookie});
        TokenDto result = authCookieService.getTokensCookie(mockRequest);
        assertNotNull(result);
        assertEquals(ACCESS_TOKEN_VALUE, result.getAccess_token());
        assertEquals("", result.getRefresh_token()); // По умолчанию пустая строка
    }

    @Test
    void getTokensCookie_OnlyRefreshTokenPresent_ShouldReturnDtoWithRefreshTokenOnly() {
        Cookie refreshCookie = new Cookie(REFRESH_TOKEN_NAME, REFRESH_TOKEN_VALUE);
        when(mockRequest.getCookies()).thenReturn(new Cookie[]{refreshCookie});
        TokenDto result = authCookieService.getTokensCookie(mockRequest);
        assertNotNull(result);
        assertEquals("", result.getAccess_token()); // По умолчанию пустая строка
        assertEquals(REFRESH_TOKEN_VALUE, result.getRefresh_token());
    }

    @Test
    void getTokensCookie_NoRelevantCookiesPresent_ShouldReturnDtoWithEmptyTokens() {
        Cookie otherCookie = new Cookie("other", "value");
        when(mockRequest.getCookies()).thenReturn(new Cookie[]{otherCookie});
        TokenDto result = authCookieService.getTokensCookie(mockRequest);
        assertNotNull(result);
        assertEquals("", result.getAccess_token());
        assertEquals("", result.getRefresh_token());
    }

    @Test
    void getTokensCookie_RequestReturnsNullCookies_ShouldReturnDtoWithEmptyTokens() {
        when(mockRequest.getCookies()).thenReturn(null); // getCookies() может вернуть null
        TokenDto result = authCookieService.getTokensCookie(mockRequest);
        assertNotNull(result);
        assertEquals("", result.getAccess_token());
        assertEquals("", result.getRefresh_token());
    }

    @Test
    void getTokensCookie_RequestReturnsEmptyCookieArray_ShouldReturnDtoWithEmptyTokens() {
        when(mockRequest.getCookies()).thenReturn(new Cookie[0]); // Пустой массив
        TokenDto result = authCookieService.getTokensCookie(mockRequest);
        assertNotNull(result);
        assertEquals("", result.getAccess_token());
        assertEquals("", result.getRefresh_token());
    }

    // --- Тесты для ExpireTokenCookie ---

    @Test
    void expireTokenCookie_ShouldAddTwoCookiesWithZeroMaxAgeAndEmptyValue() {
        // Arrange
        doNothing().when(mockResponse).addCookie(any(Cookie.class));

        // Act
        authCookieService.ExpireTokenCookie(mockResponse);

        // Assert
        verify(mockResponse, times(2)).addCookie(cookieCaptor.capture());
        List<Cookie> capturedCookies = cookieCaptor.getAllValues();

        Cookie accessTokenCookie = capturedCookies.stream()
                .filter(c -> ACCESS_TOKEN_NAME.equals(c.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Access token cookie for expiration not found"));

        Cookie refreshTokenCookie = capturedCookies.stream()
                .filter(c -> REFRESH_TOKEN_NAME.equals(c.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Refresh token cookie for expiration not found"));

        // Проверка Access Token Cookie
        assertEquals("", accessTokenCookie.getValue());
        assertTrue(accessTokenCookie.isHttpOnly());
        assertTrue(accessTokenCookie.getSecure());
        assertEquals("/", accessTokenCookie.getPath());
        assertEquals(0, accessTokenCookie.getMaxAge());

        // Проверка Refresh Token Cookie
        assertEquals("", refreshTokenCookie.getValue());
        assertTrue(refreshTokenCookie.isHttpOnly());
        assertTrue(refreshTokenCookie.getSecure());
        assertEquals("/", refreshTokenCookie.getPath());
        assertEquals(0, refreshTokenCookie.getMaxAge());
    }
}
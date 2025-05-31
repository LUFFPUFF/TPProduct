package com.example.domain.api.authentication_module.controller;

import com.example.domain.api.authentication_module.dto.AuthDataDto;
import com.example.domain.api.authentication_module.exception_handler_auth.NotFoundUserException;
import com.example.domain.api.authentication_module.exception_handler_auth.WrongPasswordException;
import com.example.domain.api.authentication_module.security.jwtUtils.AuthCookieService;
import com.example.domain.api.authentication_module.service.interfaces.AuthService;
import com.example.domain.dto.LoginReqDto;
import com.example.domain.dto.TokenDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Unit Tests")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private AuthCookieService authCookieService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginEndpoint {

        @Test
        @DisplayName("should return OK and EmailDto with tokens in cookies on successful login")
        void login_successful() throws Exception {
            // Given
            LoginReqDto loginReqDto = new LoginReqDto();
            loginReqDto.setEmail("test@example.com");
            loginReqDto.setPassword("password");

            TokenDto tokenDto = TokenDto.builder()
                    .access_token("mockAccessToken")
                    .refresh_token("mockRefreshToken")
                    .build();

            when(authService.login(loginReqDto.getEmail(), loginReqDto.getPassword())).thenReturn(tokenDto);
            doNothing().when(authCookieService).setTokenCookies(any(), eq(tokenDto));

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginReqDto)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.email").value(loginReqDto.getEmail()));

            // Verify
            verify(authService, times(1)).login(loginReqDto.getEmail(), loginReqDto.getPassword());
            verify(authCookieService, times(1)).setTokenCookies(any(), eq(tokenDto));
        }


        @Test
        @DisplayName("should throw ServletException wrapping NotFoundUserException when user does not exist")
        void login_userNotFound() throws Exception {
            // Given
            LoginReqDto loginReqDto = new LoginReqDto();
            loginReqDto.setEmail("unknown@example.com");
            loginReqDto.setPassword("password");

            NotFoundUserException expectedRootCause = new NotFoundUserException();
            when(authService.login(loginReqDto.getEmail(), loginReqDto.getPassword()))
                    .thenThrow(expectedRootCause);

            // When & Then
            Exception caughtException = null;
            try {
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginReqDto)))
                        .andReturn(); // Эта строка не будет достигнута, если perform бросает исключение
            } catch (ServletException e) { // Ловим конкретно ServletException
                caughtException = e;
            }

            assertNotNull(caughtException, "Expected a ServletException to be thrown");
            assertInstanceOf(ServletException.class, caughtException, "Exception should be ServletException");

            Throwable cause = caughtException.getCause();
            assertNotNull(cause, "ServletException should have a cause");
            assertInstanceOf(NotFoundUserException.class, cause, "Cause should be NotFoundUserException");

            NotFoundUserException actualCause = (NotFoundUserException) cause;
            assertEquals(expectedRootCause.getMessage(), actualCause.getMessage());

            // Verify
            verify(authService, times(1)).login(loginReqDto.getEmail(), loginReqDto.getPassword());
            verifyNoInteractions(authCookieService);
        }

        @Test
        @DisplayName("should throw ServletException wrapping WrongPasswordException on wrong password")
        void login_wrongPassword() throws Exception {
            // Given
            LoginReqDto loginReqDto = new LoginReqDto();
            loginReqDto.setEmail("test@example.com");
            loginReqDto.setPassword("wrongpassword");

            WrongPasswordException expectedRootCause = new WrongPasswordException();
            when(authService.login(loginReqDto.getEmail(), loginReqDto.getPassword()))
                    .thenThrow(expectedRootCause);

            // When & Then
            Exception caughtException = null;
            try {
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginReqDto)))
                        .andReturn();
            } catch (ServletException e) {
                caughtException = e;
            }

            assertNotNull(caughtException, "Expected a ServletException to be thrown");
            assertInstanceOf(ServletException.class, caughtException, "Exception should be ServletException");

            Throwable cause = caughtException.getCause();
            assertNotNull(cause, "ServletException should have a cause");
            assertInstanceOf(WrongPasswordException.class, cause, "Cause should be WrongPasswordException");

            WrongPasswordException actualCause = (WrongPasswordException) cause;
            assertEquals(expectedRootCause.getMessage(), actualCause.getMessage());

            // Verify
            verify(authService, times(1)).login(loginReqDto.getEmail(), loginReqDto.getPassword());
            verifyNoInteractions(authCookieService);
        }

        @Test
        @DisplayName("should return 400 Bad Request for invalid email format")
        void login_invalidEmailFormat() throws Exception {
            // Given
            LoginReqDto loginReqDto = new LoginReqDto();
            loginReqDto.setEmail("invalid-email");
            loginReqDto.setPassword("password");

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginReqDto)))
                    .andExpect(status().isBadRequest());

            // Verify
            verifyNoInteractions(authService);
            verifyNoInteractions(authCookieService);
        }

        @Test
        @DisplayName("should return 400 Bad Request for blank password")
        void login_blankPassword() throws Exception {
            // Given
            LoginReqDto loginReqDto = new LoginReqDto();
            loginReqDto.setEmail("test@example.com");
            loginReqDto.setPassword("");

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginReqDto)))
                    .andExpect(status().isBadRequest());
            // Verify
            verifyNoInteractions(authService);
            verifyNoInteractions(authCookieService);
        }

    }


    @Nested
    @DisplayName("POST /api/auth/logout")
    class LogoutEndpoint {
        @Test
        @DisplayName("should return OK and true, and expire cookies on successful logout")
        void logout_successful() throws Exception {
            // Given
            String mockRefreshToken = "mockRefreshTokenValue";
            TokenDto tokenInCookie = TokenDto.builder().refresh_token(mockRefreshToken).access_token("anyAccessToken").build();

            when(authCookieService.getTokensCookie(any())).thenReturn(tokenInCookie);
            when(authService.logout(mockRefreshToken)).thenReturn(true);
            doNothing().when(authCookieService).ExpireTokenCookie(any());

            // When & Then
            mockMvc.perform(post("/api/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").value(true));

            // Verify
            verify(authCookieService, times(1)).getTokensCookie(any());
            verify(authService, times(1)).logout(mockRefreshToken);
            verify(authCookieService, times(1)).ExpireTokenCookie(any());
        }

        @Test
        @DisplayName("should handle logout when refresh token is not present in cookies")
        void logout_noRefreshTokenCookie() throws Exception {
            // Given
            TokenDto emptyTokenDto = TokenDto.builder().refresh_token(null).access_token(null).build();
            when(authCookieService.getTokensCookie(any())).thenReturn(emptyTokenDto);
            when(authService.logout(null)).thenReturn(true);
            doNothing().when(authCookieService).ExpireTokenCookie(any());

            // When & Then
            mockMvc.perform(post("/api/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(true));

            // Verify
            verify(authCookieService, times(1)).getTokensCookie(any());
            verify(authService, times(1)).logout(null);
            verify(authCookieService, times(1)).ExpireTokenCookie(any());
        }
    }


    @Nested
    @DisplayName("GET /api/auth/data")
    class GetDataEndpoint {

        @Test
        @DisplayName("should return Created (201) and AuthDataDto on successful data retrieval")
        void getData_successful() throws Exception {
            // Given
            AuthDataDto expectedAuthData = AuthDataDto.builder()
                    .email("test@example.com")
                    .roles(List.of("USER", "OPERATOR"))
                    .build();

            when(authService.getData(any())).thenReturn(expectedAuthData);

            // When & Then
            mockMvc.perform(get("/api/auth/data")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.email").value(expectedAuthData.getEmail()))
                    .andExpect(jsonPath("$.roles[0]").value("USER"))
                    .andExpect(jsonPath("$.roles[1]").value("OPERATOR"));

            // Verify
            verify(authService, times(1)).getData(any());
        }

        @Test
        @DisplayName("should throw ServletException wrapping RuntimeException when data retrieval fails")
        void getData_unauthenticatedOrError() throws Exception {
            // Given
            String errorMessage = "User not authenticated or data unavailable";
            RuntimeException expectedRootCause = new RuntimeException(errorMessage);
            when(authService.getData(any())).thenThrow(expectedRootCause);

            // When & Then
            Exception caughtException = null;
            try {
                mockMvc.perform(get("/api/auth/data")
                                .accept(MediaType.APPLICATION_JSON))
                        .andReturn();
            } catch (ServletException e) {
                caughtException = e;
            }

            assertNotNull(caughtException, "Expected a ServletException to be thrown");
            assertInstanceOf(ServletException.class, caughtException, "Exception should be ServletException");

            Throwable cause = caughtException.getCause();
            assertNotNull(cause, "ServletException should have a cause");
            assertInstanceOf(RuntimeException.class, cause, "Cause should be RuntimeException");

            RuntimeException actualCause = (RuntimeException) cause;
            assertEquals(errorMessage, actualCause.getMessage());

            // Verify
            verify(authService, times(1)).getData(any());
        }
    }
}
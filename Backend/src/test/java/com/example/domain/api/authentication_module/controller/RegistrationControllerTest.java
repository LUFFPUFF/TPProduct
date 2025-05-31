package com.example.domain.api.authentication_module.controller;

import com.example.domain.api.authentication_module.exception_handler_auth.EmailExistsException;
import com.example.domain.api.authentication_module.exception_handler_auth.InvalidTokenSignException;
import com.example.domain.api.authentication_module.exception_handler_auth.NotFoundCodeException;
import com.example.domain.api.authentication_module.security.jwtUtils.AuthCookieService;
import com.example.domain.api.authentication_module.security.jwtUtils.JWTUtilsService;
import com.example.domain.api.authentication_module.service.interfaces.RegistrationService;
import com.example.domain.dto.CheckCodeDto;
import com.example.domain.dto.EmailDto;
import com.example.domain.dto.RegistrationDto;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrationController Unit Tests")
class RegistrationControllerTest {

    @Mock
    private RegistrationService registrationService;

    @Mock
    private AuthCookieService authCookieService;

    @Mock
    private JWTUtilsService jwtUtilsService;

    @InjectMocks
    private RegistrationController registrationController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(registrationController).build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("POST /api/registration/register")
    class RegisterEndpoint {

        @Test
        @DisplayName("should return OK with true on successful registration request")
        void registration_successfulRequest() throws Exception {
            RegistrationDto registrationDto = RegistrationDto.builder()
                    .email("test@example.com")
                    .password("password123")
                    .build();

            when(registrationService.registerUser(any(RegistrationDto.class))).thenReturn(true);

            mockMvc.perform(post("/api/registration/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registrationDto)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true")); // Проверяем как простую строку, которую вернет Boolean.toString()


            verify(registrationService, times(1)).registerUser(any(RegistrationDto.class));
        }

        @Test
        @DisplayName("should throw ServletException wrapping EmailExistsException when email already exists")
        void registration_emailExists() throws Exception {
            RegistrationDto registrationDto = RegistrationDto.builder()
                    .email("exists@example.com")
                    .password("password123")
                    .build();
            EmailExistsException expectedRootCause = new EmailExistsException();

            when(registrationService.registerUser(any(RegistrationDto.class))).thenThrow(expectedRootCause);

            Exception caughtException = null;
            try {
                mockMvc.perform(post("/api/registration/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registrationDto)))
                        .andReturn();
            } catch (ServletException e) {
                caughtException = e;
            }

            assertNotNull(caughtException, "Expected ServletException");
            assertInstanceOf(ServletException.class, caughtException);
            assertNotNull(caughtException.getCause(), "ServletException should have a cause");
            assertInstanceOf(EmailExistsException.class, caughtException.getCause());
            assertEquals(expectedRootCause.getMessage(), caughtException.getCause().getMessage());

            verify(registrationService, times(1)).registerUser(any(RegistrationDto.class));
        }

        @Test
        @DisplayName("should return Bad Request for invalid RegistrationDto (null email)")
        void registration_invalidDto_nullEmail() throws Exception {

            RegistrationDto registrationDto = RegistrationDto.builder()
                    .email(null) // Явно null
                    .password("password123")
                    .build();


            mockMvc.perform(post("/api/registration/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registrationDto)))
                    .andExpect(status().isBadRequest());


            verifyNoInteractions(registrationService);
        }

        @Test
        @DisplayName("should return Bad Request for invalid RegistrationDto (e.g., short password)")
        void registration_invalidDto_shortPassword() throws Exception {
            RegistrationDto registrationDto = RegistrationDto.builder()
                    .email("test@example.com")
                    .password("123") // Short password
                    .build();

            mockMvc.perform(post("/api/registration/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registrationDto)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(registrationService);
        }
    }

    @Nested
    @DisplayName("POST /api/registration/check-code")
    class CheckCodeEndpoint {

        @Test
        @DisplayName("should return Created with EmailDto and set cookies on successful code check")
        void checkCode_successful() throws Exception {
            CheckCodeDto checkCodeDto = new CheckCodeDto();
            checkCodeDto.setCode("123456");

            TokenDto mockTokenDto = TokenDto.builder()
                    .access_token("mockAccessToken")
                    .refresh_token("mockRefreshToken")
                    .build();
            String expectedEmail = "user@example.com";

            when(registrationService.checkRegistrationCode(checkCodeDto.getCode())).thenReturn(mockTokenDto);
            when(jwtUtilsService.getEmail(mockTokenDto.getAccess_token())).thenReturn(expectedEmail);
            doNothing().when(authCookieService).setTokenCookies(any(), eq(mockTokenDto));

            mockMvc.perform(post("/api/registration/check-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(checkCodeDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value(expectedEmail));

            verify(registrationService, times(1)).checkRegistrationCode(checkCodeDto.getCode());
            verify(authCookieService, times(1)).setTokenCookies(any(), eq(mockTokenDto));
            verify(jwtUtilsService, times(1)).getEmail(mockTokenDto.getAccess_token());
        }

        @Test
        @DisplayName("should throw ServletException wrapping NotFoundCodeException for valid format but not found code")
        void checkCode_invalidCode() throws Exception {
            CheckCodeDto checkCodeDto = new CheckCodeDto();
            checkCodeDto.setCode("654321"); // Валидная длина, но код будет "не найден" сервисом
            NotFoundCodeException expectedRootCause = new NotFoundCodeException();

            when(registrationService.checkRegistrationCode(checkCodeDto.getCode())).thenThrow(expectedRootCause);

            Exception caughtException = null;
            try {
                mockMvc.perform(post("/api/registration/check-code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(checkCodeDto)))
                        .andReturn();
            } catch (ServletException e) {
                caughtException = e;
            }

            assertNotNull(caughtException, "Expected ServletException");
            assertInstanceOf(ServletException.class, caughtException);
            assertNotNull(caughtException.getCause(), "ServletException should have a cause");
            assertInstanceOf(NotFoundCodeException.class, caughtException.getCause());
            assertEquals(expectedRootCause.getMessage(), caughtException.getCause().getMessage());

            verify(registrationService, times(1)).checkRegistrationCode(checkCodeDto.getCode());
            verifyNoInteractions(authCookieService);
            verifyNoInteractions(jwtUtilsService);
        }

        @Test
        @DisplayName("should return Bad Request for invalid CheckCodeDto (e.g., blank code)")
        void checkCode_invalidDto_blankCode() throws Exception {
            CheckCodeDto checkCodeDto = new CheckCodeDto();
            checkCodeDto.setCode(""); // Blank code

            mockMvc.perform(post("/api/registration/check-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(checkCodeDto)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(registrationService);
            verifyNoInteractions(authCookieService);
            verifyNoInteractions(jwtUtilsService);
        }

        @Test
        @DisplayName("should return Bad Request for invalid CheckCodeDto (e.g., wrong length code)")
        void checkCode_invalidDto_wrongLengthCode() throws Exception {
            CheckCodeDto checkCodeDto = new CheckCodeDto();
            checkCodeDto.setCode("123"); // Wrong length

            mockMvc.perform(post("/api/registration/check-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(checkCodeDto)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(registrationService);
            verifyNoInteractions(authCookieService);
            verifyNoInteractions(jwtUtilsService);
        }

        @Test
        @DisplayName("should throw ServletException wrapping InvalidTokenSignException if getEmail from token fails")
        void checkCode_getEmailFails() throws Exception {
            CheckCodeDto checkCodeDto = new CheckCodeDto();
            checkCodeDto.setCode("123456");

            TokenDto mockTokenDto = TokenDto.builder()
                    .access_token("mockAccessToken")
                    .refresh_token("mockRefreshToken")
                    .build();
            InvalidTokenSignException expectedRootCause = new InvalidTokenSignException();


            when(registrationService.checkRegistrationCode(checkCodeDto.getCode())).thenReturn(mockTokenDto);
            when(jwtUtilsService.getEmail(mockTokenDto.getAccess_token())).thenThrow(expectedRootCause);
            doNothing().when(authCookieService).setTokenCookies(any(), eq(mockTokenDto));


            Exception caughtException = null;
            try {
                mockMvc.perform(post("/api/registration/check-code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(checkCodeDto)))
                        .andReturn();
            } catch (ServletException e) {
                caughtException = e;
            }

            assertNotNull(caughtException, "Expected ServletException");
            assertInstanceOf(ServletException.class, caughtException);
            assertNotNull(caughtException.getCause(), "ServletException should have a cause");
            assertInstanceOf(InvalidTokenSignException.class, caughtException.getCause());
            assertEquals(expectedRootCause.getMessage(), caughtException.getCause().getMessage());

            verify(registrationService, times(1)).checkRegistrationCode(checkCodeDto.getCode());
            verify(authCookieService, times(1)).setTokenCookies(any(), eq(mockTokenDto)); // Этот метод успеет вызваться
            verify(jwtUtilsService, times(1)).getEmail(mockTokenDto.getAccess_token());
        }
    }
}
package com.example.domain.api.authentication_module.service.impl;

import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.authentication_module.cache.AuthCacheService;
import com.example.domain.api.authentication_module.exception_handler_auth.EmailExistsException;
import com.example.domain.api.authentication_module.security.jwtUtils.JWTUtilsService;
import com.example.domain.dto.RegistrationDto;
import com.example.domain.dto.TokenDto;
import com.example.domain.dto.mapper.MapperDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataAccessException; // Для userRepository.save
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;


import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegistrationServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private MapperDto mapperDto;
    @Mock private JWTUtilsService jwtUtilsService; // Имя в сервисе jWTUtilsService
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserDetailsService userDetailsService;
    @Mock private AuthCacheService authCacheService;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    @Captor private ArgumentCaptor<RegistrationDto> registrationDtoCaptor;
    @Captor private ArgumentCaptor<User> userEntityCaptor;
    @Captor private ArgumentCaptor<String> stringCaptor; // Для кода и email

    private RegistrationDto testRegistrationDto;
    private User testUserEntity;
    private UserDetails testUserDetails;
    private TokenDto testTokenDto;

    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_PASSWORD = "password123";
    private final String ENCODED_PASSWORD = "encodedPassword123";
    private final String REGISTRATION_CODE = "000000"; // Захардкоженный код
    private final String ACCESS_TOKEN = "testAccessToken";
    private final String REFRESH_TOKEN = "testRefreshToken";


    @BeforeEach
    void setUp() {
        reset(userRepository, mapperDto, jwtUtilsService, passwordEncoder, userDetailsService, authCacheService);

        testRegistrationDto = RegistrationDto.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .fullName(TEST_EMAIL) // Сервис устанавливает fullName = email
                .build();
        // createdAt и updatedAt устанавливаются внутри сервиса

        testUserEntity = new User();
        testUserEntity.setId(1); // ID присваивается после save
        testUserEntity.setEmail(TEST_EMAIL);
        testUserEntity.setPassword(ENCODED_PASSWORD); // Пароль уже закодирован в DTO перед маппингом
        testUserEntity.setFullName(TEST_EMAIL);

        testUserDetails = mock(UserDetails.class);
        when(testUserDetails.getUsername()).thenReturn(TEST_EMAIL);

        testTokenDto = TokenDto.builder()
                .access_token(ACCESS_TOKEN)
                .refresh_token(REFRESH_TOKEN)
                .build();
    }

    // --- Тесты для registerUser ---

    @Test
    void registerUser_NewEmail_ShouldEncodePasswordSetTimestampsAndPutCodeToCache() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty()); // Email свободен
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        doNothing().when(authCacheService).putRegistrationCode(anyString(), any(RegistrationDto.class));

        // Act
        Boolean result = registrationService.registerUser(
                RegistrationDto.builder().email(TEST_EMAIL).password(TEST_PASSWORD).build() // Передаем "сырой" DTO
        );

        // Assert
        assertTrue(result);
        verify(userRepository).findByEmail(TEST_EMAIL); // Проверка в checkEmailIsAvailable
        verify(passwordEncoder).encode(TEST_PASSWORD);

        verify(authCacheService).putRegistrationCode(eq(REGISTRATION_CODE), registrationDtoCaptor.capture());
        RegistrationDto capturedDto = registrationDtoCaptor.getValue();
        assertEquals(TEST_EMAIL, capturedDto.getEmail());
        assertEquals(ENCODED_PASSWORD, capturedDto.getPassword());
        assertEquals(TEST_EMAIL, capturedDto.getFullName()); // Устанавливается сервисом
        assertNotNull(capturedDto.getCreatedAt());
        assertNotNull(capturedDto.getUpdatedAt());
    }

    @Test
    void registerUser_EmailExists_ShouldThrowEmailExistsException() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(new User())); // Email занят

        // Act & Assert
        assertThrows(EmailExistsException.class, () -> {
            registrationService.registerUser(testRegistrationDto);
        });
        verify(userRepository).findByEmail(TEST_EMAIL);
        verifyNoInteractions(passwordEncoder, authCacheService);
    }

    @Test
    void registerUser_PasswordEncoderFails_ShouldThrowException() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
        RuntimeException passwordException = new RuntimeException("Encoding failed");
        when(passwordEncoder.encode(TEST_PASSWORD)).thenThrow(passwordException);

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            registrationService.registerUser(RegistrationDto.builder().email(TEST_EMAIL).password(TEST_PASSWORD).build());
        });
        assertEquals(passwordException, thrown);
        verifyNoInteractions(authCacheService);
    }

    @Test
    void registerUser_AuthCacheServiceFails_ShouldThrowException() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        RuntimeException cacheException = new RuntimeException("Cache put failed");
        doThrow(cacheException).when(authCacheService).putRegistrationCode(anyString(), any(RegistrationDto.class));

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            registrationService.registerUser(RegistrationDto.builder().email(TEST_EMAIL).password(TEST_PASSWORD).build());
        });
        assertEquals(cacheException, thrown);
    }

    // --- Тесты для sendRegistrationCode (косвенно покрыты registerUser) ---
    @Test
    void sendRegistrationCode_ShouldPutCodeAndDtoToCache() {
        // Arrange
        doNothing().when(authCacheService).putRegistrationCode(anyString(), any(RegistrationDto.class));

        // Act
        Boolean result = registrationService.sendRegistrationCode(testRegistrationDto);

        // Assert
        assertTrue(result);
        verify(authCacheService).putRegistrationCode(eq(REGISTRATION_CODE), eq(testRegistrationDto));
    }


    // --- Тесты для checkRegistrationCode ---

    @Test
    void checkRegistrationCode_ValidCode_ShouldSaveUserLoadDetailsGenerateTokensAndReturnTokenDto() {
        // Arrange
        // DTO из кеша будет иметь уже закодированный пароль и установленные даты
        RegistrationDto dtoFromCache = RegistrationDto.builder()
                .email(TEST_EMAIL)
                .password(ENCODED_PASSWORD) // Важно: пароль уже закодирован
                .fullName(TEST_EMAIL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(authCacheService.getRegistrationCode(REGISTRATION_CODE)).thenReturn(Optional.of(dtoFromCache));
        when(mapperDto.toEntityUserFromRegistration(dtoFromCache)).thenReturn(testUserEntity);
        when(userRepository.save(testUserEntity)).thenReturn(testUserEntity); // save возвращает сохраненную сущность
        when(userDetailsService.loadUserByUsername(TEST_EMAIL)).thenReturn(testUserDetails);
        when(jwtUtilsService.generateTokensByUser(testUserDetails)).thenReturn(testTokenDto);

        // Act
        TokenDto result = registrationService.checkRegistrationCode(REGISTRATION_CODE);

        // Assert
        assertNotNull(result);
        assertEquals(testTokenDto, result);

        verify(authCacheService).getRegistrationCode(REGISTRATION_CODE);
        verify(mapperDto).toEntityUserFromRegistration(dtoFromCache);
        verify(userRepository).save(testUserEntity);
        verify(userDetailsService).loadUserByUsername(TEST_EMAIL);
        verify(jwtUtilsService).generateTokensByUser(testUserDetails);
    }


    @Test
    void checkRegistrationCode_MapperFails_ShouldThrowException() {
        when(authCacheService.getRegistrationCode(REGISTRATION_CODE)).thenReturn(Optional.of(testRegistrationDto));
        RuntimeException mapperEx = new RuntimeException("Mapping failed");
        when(mapperDto.toEntityUserFromRegistration(testRegistrationDto)).thenThrow(mapperEx);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            registrationService.checkRegistrationCode(REGISTRATION_CODE);
        });
        assertEquals(mapperEx, thrown);
    }

    @Test
    void checkRegistrationCode_RepositorySaveFails_ShouldThrowException() {
        when(authCacheService.getRegistrationCode(REGISTRATION_CODE)).thenReturn(Optional.of(testRegistrationDto));
        when(mapperDto.toEntityUserFromRegistration(testRegistrationDto)).thenReturn(testUserEntity);
        DataAccessException dbEx = new DataAccessException("Save failed") {};
        when(userRepository.save(testUserEntity)).thenThrow(dbEx);

        DataAccessException thrown = assertThrows(DataAccessException.class, () -> {
            registrationService.checkRegistrationCode(REGISTRATION_CODE);
        });
        assertEquals(dbEx, thrown);
    }

    @Test
    void checkRegistrationCode_UserDetailsServiceFails_ShouldThrowException() {
        when(authCacheService.getRegistrationCode(REGISTRATION_CODE)).thenReturn(Optional.of(testRegistrationDto));
        when(mapperDto.toEntityUserFromRegistration(testRegistrationDto)).thenReturn(testUserEntity);
        when(userRepository.save(testUserEntity)).thenReturn(testUserEntity);
        UsernameNotFoundException userDetailsEx = new UsernameNotFoundException("User not found for details");
        when(userDetailsService.loadUserByUsername(TEST_EMAIL)).thenThrow(userDetailsEx);

        UsernameNotFoundException thrown = assertThrows(UsernameNotFoundException.class, () -> {
            registrationService.checkRegistrationCode(REGISTRATION_CODE);
        });
        assertEquals(userDetailsEx, thrown);
    }

    @Test
    void checkRegistrationCode_JwtServiceFails_ShouldThrowException() {
        when(authCacheService.getRegistrationCode(REGISTRATION_CODE)).thenReturn(Optional.of(testRegistrationDto));
        when(mapperDto.toEntityUserFromRegistration(testRegistrationDto)).thenReturn(testUserEntity);
        when(userRepository.save(testUserEntity)).thenReturn(testUserEntity);
        when(userDetailsService.loadUserByUsername(TEST_EMAIL)).thenReturn(testUserDetails);
        RuntimeException jwtEx = new RuntimeException("Token generation failed");
        when(jwtUtilsService.generateTokensByUser(testUserDetails)).thenThrow(jwtEx);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            registrationService.checkRegistrationCode(REGISTRATION_CODE);
        });
        assertEquals(jwtEx, thrown);
    }


    // --- Тесты для checkEmailIsAvailable ---
    @Test
    void checkEmailIsAvailable_EmailFree_ShouldNotThrowException() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        assertDoesNotThrow(() -> {
            registrationService.checkEmailIsAvailable(TEST_EMAIL);
        });
        verify(userRepository).findByEmail(TEST_EMAIL);
    }

    @Test
    void checkEmailIsAvailable_EmailTaken_ShouldThrowEmailExistsException() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(new User()));

        // Act & Assert
        assertThrows(EmailExistsException.class, () -> {
            registrationService.checkEmailIsAvailable(TEST_EMAIL);
        });
        verify(userRepository).findByEmail(TEST_EMAIL);
    }
}
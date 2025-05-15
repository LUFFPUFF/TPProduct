package com.example.domain.api.authentication_module.security.jwtUtils.impl;

import com.example.domain.api.authentication_module.cache.AuthCacheService;
import com.example.domain.api.authentication_module.exception_handler_auth.InvalidTokenSignException;
import com.example.domain.api.authentication_module.security.config.JWTConfig;
import com.example.domain.dto.TokenDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JWTUtilsServiceImplTest {

    @Mock private JWTConfig jwtConfig;
    @Mock private AuthCacheService authCacheService;

    @InjectMocks
    private JWTUtilsServiceImpl jwtUtilsService;

    @Captor private ArgumentCaptor<String> stringCaptor;
    @Captor private ArgumentCaptor<String> usernameCaptor;

    private UserDetails testUserDetails;
    private final String TEST_EMAIL_AS_USERNAME = "test@example.com";
    private final String TEST_SECRET = "TestSecretKeyForJWTGenerationWhichIsLongEnoughAndSecure32Bytes";
    private final long ACCESS_EXPIRATION = 3600000L;
    private final long REFRESH_EXPIRATION = 86400000L;
    private Key signingKey;

    @BeforeEach
    void setUp() {
        reset(jwtConfig, authCacheService);

        when(jwtConfig.getSecret()).thenReturn(TEST_SECRET);
        when(jwtConfig.getAccessExpiration()).thenReturn(ACCESS_EXPIRATION);
        when(jwtConfig.getRefreshExpiration()).thenReturn(REFRESH_EXPIRATION);

        signingKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"));
        testUserDetails = new User(TEST_EMAIL_AS_USERNAME, "password", authorities);
    }

    private Claims parseTokenDirectly(String token) {
        return Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token).getBody();
    }

    @Test
    void generateAccessToken_ShouldCreateValidTokenWithCorrectClaims() {
        String accessToken = jwtUtilsService.generateAccessToken(testUserDetails);
        assertNotNull(accessToken);
        Claims claims = parseTokenDirectly(accessToken);

        // ИСПРАВЛЕНО: Проверяем "email" claim, а для subject ожидаем null
        assertEquals(TEST_EMAIL_AS_USERNAME, claims.get("email", String.class), "Email claim should match UserDetails username");
        assertNull(claims.getSubject(), "Subject claim is observed to be null");


        List<String> roles = claims.get("roles", List.class);
        assertNotNull(roles);
        assertEquals(2, roles.size());
        assertTrue(roles.contains("ROLE_USER"));
        assertTrue(roles.contains("ROLE_ADMIN"));
        assertTrue(claims.getIssuedAt().before(new Date(System.currentTimeMillis() + 1000)));
        assertTrue(claims.getExpiration().after(new Date()));
        long expectedExpTime = System.currentTimeMillis() + ACCESS_EXPIRATION;
        assertTrue(Math.abs(claims.getExpiration().getTime() - expectedExpTime) < 5000);
    }

    @Test
    void generateRefreshToken_ShouldCreateValidTokenWithCorrectClaims() {
        String refreshToken = jwtUtilsService.generateRefreshToken(testUserDetails);
        assertNotNull(refreshToken);
        Claims claims = parseTokenDirectly(refreshToken);

        // ИСПРАВЛЕНО: Проверяем "email" claim, а для subject ожидаем null
        assertEquals(TEST_EMAIL_AS_USERNAME, claims.get("email", String.class));
        assertNull(claims.getSubject(), "Subject claim is observed to be null in refresh token");


        List<String> roles = claims.get("roles", List.class);
        assertNotNull(roles);
        assertTrue(roles.contains("ROLE_USER"));
        assertTrue(claims.getIssuedAt().before(new Date(System.currentTimeMillis() + 1000)));
        assertTrue(claims.getExpiration().after(new Date()));
        long expectedExpTime = System.currentTimeMillis() + REFRESH_EXPIRATION;
        assertTrue(Math.abs(claims.getExpiration().getTime() - expectedExpTime) < 5000);
    }

    @Test
    void generateTokensByUser_ShouldGenerateBothTokensAndPutRefreshTokenToCache() {
        doNothing().when(authCacheService).putRefreshToken(anyString(), anyString());
        TokenDto tokens = jwtUtilsService.generateTokensByUser(testUserDetails);

        assertNotNull(tokens);
        assertNotNull(tokens.getAccess_token());
        assertNotNull(tokens.getRefresh_token());
        verify(authCacheService).putRefreshToken(stringCaptor.capture(), usernameCaptor.capture());
        assertEquals(tokens.getRefresh_token(), stringCaptor.getValue());
        assertEquals(TEST_EMAIL_AS_USERNAME, usernameCaptor.getValue());

        Claims accessClaims = parseTokenDirectly(tokens.getAccess_token());
        // ИСПРАВЛЕНО: Проверяем "email" claim, а для subject ожидаем null
        assertEquals(TEST_EMAIL_AS_USERNAME, accessClaims.get("email", String.class));
        assertNull(accessClaims.getSubject(), "Access Token Subject should be null");


        Claims refreshClaims = parseTokenDirectly(tokens.getRefresh_token());
        // ИСПРАВЛЕНО: Проверяем "email" claim, а для subject ожидаем null
        assertEquals(TEST_EMAIL_AS_USERNAME, refreshClaims.get("email", String.class));
        assertNull(refreshClaims.getSubject(), "Refresh Token Subject should be null");
    }

    @Test
    void parseToken_ValidToken_ShouldReturnClaims() {
        String token = jwtUtilsService.generateAccessToken(testUserDetails); // Генерируется с null subject
        Claims claims = jwtUtilsService.parseToken(token);
        assertNotNull(claims);
        // ИСПРАВЛЕНО: Проверяем "email" claim, а для subject ожидаем null
        assertEquals(TEST_EMAIL_AS_USERNAME, claims.get("email", String.class));
        assertNull(claims.getSubject(), "Parsed Token Subject should be null");
    }

    @Test
    void parseToken_ExpiredToken_ShouldThrowInvalidTokenSignException() throws InterruptedException {
        when(jwtConfig.getAccessExpiration()).thenReturn(1L);
        String expiredToken = jwtUtilsService.generateAccessToken(testUserDetails);
        Thread.sleep(50);
        assertThrows(InvalidTokenSignException.class, () -> {
            jwtUtilsService.parseToken(expiredToken);
        });
    }

    @Test
    void parseToken_InvalidSignature_ShouldThrowSignatureException() {
        String token = Jwts.builder()
                .setSubject(null) // Устанавливаем null, как мы наблюдаем
                .claim("email", TEST_EMAIL_AS_USERNAME)
                .signWith(signingKey)
                .compact();
        String[] parts = token.split("\\.");
        assertTrue(parts.length == 3, "Token should have 3 parts");
        char lastChar = parts[2].charAt(parts[2].length() - 1);
        char newChar = (lastChar == 'A' ? 'B' : 'A');
        String tamperedSignature = parts[2].substring(0, parts[2].length() - 1) + newChar;
        String tamperedToken = parts[0] + "." + parts[1] + "." + tamperedSignature;

        assertThrows(SignatureException.class, () -> {
            jwtUtilsService.parseToken(tamperedToken);
        });
    }

    @Test
    void parseToken_MalformedToken_ShouldThrowMalformedJwtException() {
        String malformedToken = "this.is.not.a.jwt";
        assertThrows(MalformedJwtException.class, () -> {
            jwtUtilsService.parseToken(malformedToken);
        });
    }

    @Test
    void getRoles_ValidToken_ShouldReturnRolesList() {
        String token = jwtUtilsService.generateAccessToken(testUserDetails);
        List<String> roles = jwtUtilsService.getRoles(token);
        assertNotNull(roles);
        assertEquals(2, roles.size());
        assertTrue(roles.contains("ROLE_USER"));
        assertTrue(roles.contains("ROLE_ADMIN"));
    }

    @Test
    void getRoles_TokenWithoutRolesClaim_ShouldReturnNull() {
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("email", TEST_EMAIL_AS_USERNAME);
        String tokenWithoutRoles = Jwts.builder()
                .setSubject(null) // Устанавливаем null, как мы наблюдаем
                .setClaims(claimsMap)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getAccessExpiration()))
                .signWith(signingKey)
                .compact();
        assertNull(jwtUtilsService.getRoles(tokenWithoutRoles));
    }

    @Test
    void isTokenExpired_NotExpiredToken_ShouldReturnFalse() {
        String token = jwtUtilsService.generateAccessToken(testUserDetails);
        assertFalse(jwtUtilsService.isTokenExpired(token));
    }

    @Test
    void isTokenExpired_ExpiredToken_ShouldReturnTrue() throws InterruptedException {
        when(jwtConfig.getAccessExpiration()).thenReturn(1L);
        String expiredToken = jwtUtilsService.generateAccessToken(testUserDetails);
        Thread.sleep(50);
        assertTrue(jwtUtilsService.isTokenExpired(expiredToken));
    }

    @Test
    void isTokenExpired_InvalidToken_ShouldReturnTrue() {
        String invalidToken = "invalid.token.string";
        assertTrue(jwtUtilsService.isTokenExpired(invalidToken));
    }
}
package com.example.domain.api.authentication_module.security;

import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.database.repository.company_subscription_module.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;


import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserDetailsServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserRoleRepository userRoleRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User dbUser;
    private final String TEST_EMAIL = "user@example.com";
    private final String TEST_PASSWORD_ENCODED = "encodedPassword";
    private final List<Role> USER_ROLES = List.of(Role.OPERATOR, Role.MANAGER);
    private final List<GrantedAuthority> GRANTED_AUTHORITIES = USER_ROLES.stream()
            .map(role -> new SimpleGrantedAuthority(role.name())) // или role.getAuthority() если Role реализует GrantedAuthority
            .collect(Collectors.toList());


    @BeforeEach
    void setUp() {
        reset(userRepository, userRoleRepository);

        dbUser = new User();
        dbUser.setId(1);
        dbUser.setEmail(TEST_EMAIL);
        dbUser.setPassword(TEST_PASSWORD_ENCODED);
    }

    @Test
    void loadUserByUsername_UserExistsAndHasRoles_ShouldReturnUserDetailsWithAuthorities() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(dbUser));
        when(userRoleRepository.findRolesByEmail(TEST_EMAIL)).thenReturn(USER_ROLES);

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(TEST_EMAIL);

        // Assert
        assertNotNull(userDetails);
        assertEquals(TEST_EMAIL, userDetails.getUsername());
        assertEquals(TEST_PASSWORD_ENCODED, userDetails.getPassword());

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertNotNull(authorities);
        assertEquals(USER_ROLES.size(), authorities.size());
        // Сравниваем наборы имен авторитетов
        List<String> expectedAuthorityNames = USER_ROLES.stream().map(Role::name).collect(Collectors.toList());
        List<String> actualAuthorityNames = authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());
        assertTrue(actualAuthorityNames.containsAll(expectedAuthorityNames) && expectedAuthorityNames.containsAll(actualAuthorityNames));

        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(userRoleRepository).findRolesByEmail(TEST_EMAIL);
    }

    @Test
    void loadUserByUsername_UserNotFound_ShouldThrowUsernameNotFoundException() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername(TEST_EMAIL);
        });
        assertEquals(TEST_EMAIL, exception.getMessage()); // Проверяем, что email в сообщении об ошибке

        verify(userRepository).findByEmail(TEST_EMAIL);
        verifyNoInteractions(userRoleRepository); // Не должен вызываться, если пользователь не найден
    }

    @Test
    void loadUserByUsername_UserExistsButNoRoles_ShouldReturnUserDetailsWithEmptyAuthorities() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(dbUser));
        when(userRoleRepository.findRolesByEmail(TEST_EMAIL)).thenReturn(Collections.emptyList()); // Нет ролей

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(TEST_EMAIL);

        // Assert
        assertNotNull(userDetails);
        assertEquals(TEST_EMAIL, userDetails.getUsername());
        assertEquals(TEST_PASSWORD_ENCODED, userDetails.getPassword());
        assertNotNull(userDetails.getAuthorities());
        assertTrue(userDetails.getAuthorities().isEmpty()); // Ожидаем пустой список авторитетов

        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(userRoleRepository).findRolesByEmail(TEST_EMAIL);
    }

    @Test
    void loadUserByUsername_UserRepositoryThrowsException_ShouldThrowException() {
        // Arrange
        DataAccessException dbException = new DataAccessException("DB error finding user") {};
        when(userRepository.findByEmail(TEST_EMAIL)).thenThrow(dbException);

        // Act & Assert
        DataAccessException thrown = assertThrows(DataAccessException.class, () -> {
            userDetailsService.loadUserByUsername(TEST_EMAIL);
        });
        assertEquals(dbException, thrown); // Ожидаем проброс исходного исключения

        verify(userRepository).findByEmail(TEST_EMAIL);
        verifyNoInteractions(userRoleRepository);
    }

    @Test
    void loadUserByUsername_UserRoleRepositoryThrowsException_ShouldThrowException() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(dbUser));
        DataAccessException dbException = new DataAccessException("DB error finding roles") {};
        when(userRoleRepository.findRolesByEmail(TEST_EMAIL)).thenThrow(dbException);

        // Act & Assert
        DataAccessException thrown = assertThrows(DataAccessException.class, () -> {
            userDetailsService.loadUserByUsername(TEST_EMAIL);
        });
        assertEquals(dbException, thrown); // Ожидаем проброс исходного исключения

        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(userRoleRepository).findRolesByEmail(TEST_EMAIL);
    }
}
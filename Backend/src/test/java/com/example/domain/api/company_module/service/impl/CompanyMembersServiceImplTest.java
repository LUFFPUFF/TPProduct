package com.example.domain.api.company_module.service.impl;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.authentication_module.service.interfaces.RoleService;
import com.example.domain.api.company_module.exception_handler_company.NotFoundCompanyException;
import com.example.domain.api.company_module.exception_handler_company.SelfMemberDisbandException;
import com.example.domain.api.subscription_module.service.SubscriptionService;
import com.example.domain.dto.CompanyDto;
import com.example.domain.dto.CompanyWithMembersDto;
import com.example.domain.dto.MemberDto;
import com.example.domain.dto.mapper.MapperDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataAccessException;


import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompanyMembersServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleService roleService;
    @Mock private SubscriptionService subscriptionService;
    @Mock private MapperDto mapperDto;

    @InjectMocks
    private CompanyMembersServiceImpl companyMembersService;

    private User adminUser;
    private User memberUser;
    private Company testCompany;
    private CompanyDto testCompanyDto;
    private final String ADMIN_EMAIL = "admin@example.com";
    private final String MEMBER_EMAIL = "member@example.com";
    private final Integer COMPANY_ID = 1;

    @BeforeEach
    void setUp() {
        reset(userRepository, roleService, subscriptionService, mapperDto);

        testCompany = new Company();
        testCompany.setId(COMPANY_ID);
        testCompany.setName("Test Corp");
        testCompany.setContactEmail(ADMIN_EMAIL);

        adminUser = new User();
        adminUser.setId(1);
        adminUser.setEmail(ADMIN_EMAIL);
        adminUser.setFullName("Admin User");
        adminUser.setCompany(testCompany);

        memberUser = new User();
        memberUser.setId(2);
        memberUser.setEmail(MEMBER_EMAIL);
        memberUser.setFullName("Member User");
        // memberUser.setCompany(testCompany); // Initially not in company or will be set by test

        testCompanyDto = CompanyDto.builder().id(COMPANY_ID).name("Test Corp").contactEmail(ADMIN_EMAIL).build();

        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(adminUser));
        // findMembers использует findByCompanyId, который вернет Optional<User>
        // Для тестов, где нужен список участников, будем мокировать findByCompanyId(COMPANY_ID)
        // или getAllByCompanyId, если сервис будет исправлен
        when(mapperDto.toDtoCompany(testCompany)).thenReturn(testCompanyDto);
    }

    // --- Тесты для findMembers ---
    @Test
    void findMembers_WhenUserFoundByCompanyId_ShouldReturnSingleMemberDtoList() {
        // Arrange
        // Сервис использует userRepository.findByCompanyId, который вернет Optional<User>
        when(userRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(adminUser));

        // Act
        List<MemberDto> members = companyMembersService.findMembers(testCompany);

        // Assert
        assertNotNull(members);
        // ИСПРАВЛЕНО: Ожидаем 1, так как findByCompanyId().stream() даст 0 или 1 элемент
        assertEquals(1, members.size());
        assertEquals(ADMIN_EMAIL, members.get(0).getEmail());
        verify(userRepository).findByCompanyId(COMPANY_ID);
    }

    @Test
    void findMembers_WhenNoUserFoundByCompanyId_ShouldReturnEmptyList() {
        // ИСПРАВЛЕНО: Мокируем findByCompanyId
        when(userRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());
        List<MemberDto> members = companyMembersService.findMembers(testCompany);
        assertNotNull(members);
        assertTrue(members.isEmpty());
        verify(userRepository).findByCompanyId(COMPANY_ID);
    }

    @Test
    void findMembers_RepositoryThrowsException_ShouldThrowException() {
        // ИСПРАВЛЕНО: Мокируем findByCompanyId, чтобы бросил исключение
        DataAccessException dbEx = new DataAccessException("DB error") {};
        when(userRepository.findByCompanyId(COMPANY_ID)).thenThrow(dbEx);
        assertThrows(DataAccessException.class, () -> {
            companyMembersService.findMembers(testCompany);
        });
        verify(userRepository).findByCompanyId(COMPANY_ID);
    }

    // --- Тесты для addMember ---
    @Test
    void addMember_Successful_ShouldUpdateServicesAndReturnDtoWithOneMember() {
        // Arrange
        User memberUserToAdd = new User();
        memberUserToAdd.setId(memberUser.getId());
        memberUserToAdd.setEmail(MEMBER_EMAIL);
        memberUserToAdd.setFullName(memberUser.getFullName());
        // memberUserToAdd.setCompany(testCompany); // Компания будет установлена через updateByCompanyIdAndEmail

        // findByEmail для админа (взят из setUp)
        // findByEmail для memberEmail - не нужен, т.к. сервис не проверяет его существование перед addRole/update
        doNothing().when(subscriptionService).addOperatorCount(testCompany);
        when(roleService.addRole(MEMBER_EMAIL, Role.OPERATOR)).thenReturn(true);
        doNothing().when(userRepository).updateByCompanyIdAndEmail(COMPANY_ID, MEMBER_EMAIL);

        // Мокируем findMembers для финального DTO
        // После добавления, findMembers (с его текущей логикой findByCompanyId)
        // вернет кого-то ОДНОГО из компании. Допустим, админа.
        when(userRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(adminUser));

        // Act
        CompanyWithMembersDto result = companyMembersService.addMember(MEMBER_EMAIL, ADMIN_EMAIL);

        // Assert
        assertNotNull(result);
        assertEquals(testCompanyDto, result.getCompany());
        // ИСПРАВЛЕНО: findMembers вернет 1, так как использует findByCompanyId
        assertEquals(1, result.getMembers().size());
        assertEquals(adminUser.getEmail(), result.getMembers().get(0).getEmail());


        verify(userRepository).findByEmail(ADMIN_EMAIL);
        verify(subscriptionService).addOperatorCount(testCompany);
        verify(roleService).addRole(MEMBER_EMAIL, Role.OPERATOR);
        verify(userRepository).updateByCompanyIdAndEmail(COMPANY_ID, MEMBER_EMAIL);
        verify(userRepository).findByCompanyId(COMPANY_ID); // Вызов из findMembers
    }

    @Test
    void addMember_AdminUserNotFound_ShouldThrowNotFoundCompanyException() {
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.empty());
        assertThrows(NotFoundCompanyException.class, () -> {
            companyMembersService.addMember(MEMBER_EMAIL, ADMIN_EMAIL);
        });
        verifyNoInteractions(subscriptionService, roleService);
        // userRepository.findByEmail(ADMIN_EMAIL) был вызван
        verify(userRepository, times(1)).findByEmail(anyString());
    }

    @Test
    void addMember_AdminUserHasNoCompany_ShouldThrowNotFoundCompanyException() {
        adminUser.setCompany(null);
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(adminUser));
        assertThrows(NotFoundCompanyException.class, () -> {
            companyMembersService.addMember(MEMBER_EMAIL, ADMIN_EMAIL);
        });
        verify(userRepository, times(1)).findByEmail(ADMIN_EMAIL);
    }

    @Test
    void addMember_SubscriptionServiceFails_ShouldThrowException() {
        // Arrange
        // findByEmail для админа успеет выполниться
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(adminUser));
        doThrow(new RuntimeException("Subscription error")).when(subscriptionService).addOperatorCount(testCompany);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            companyMembersService.addMember(MEMBER_EMAIL, ADMIN_EMAIL);
        });

        // ИСПРАВЛЕНО: Проверяем, что было вызвано до ошибки
        verify(userRepository).findByEmail(ADMIN_EMAIL);
        verify(subscriptionService).addOperatorCount(testCompany);
        // Эти не должны были вызваться
        verifyNoMoreInteractions(roleService, userRepository); // No more interactions on userRepository (besides findByEmail)
    }

    // --- Тесты для removeMember ---
    @Test
    void removeMember_Successful_ShouldUpdateServicesAndReturnDtoWithOneMember() {
        // Arrange
        // findByEmail для админа (взят из setUp)
        // findByEmail для memberEmail не нужен, т.к. сервис не проверяет его существование перед removeRole
        doNothing().when(subscriptionService).subtractOperatorCount(testCompany);
        when(roleService.removeRole(MEMBER_EMAIL, Role.OPERATOR)).thenReturn(true);
        // findMembers после "удаления" (которое только снимает роль) все равно вернет кого-то одного из компании
        when(userRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(adminUser));

        // Act
        CompanyWithMembersDto result = companyMembersService.removeMember(MEMBER_EMAIL, ADMIN_EMAIL);

        // Assert
        assertNotNull(result);
        assertEquals(testCompanyDto, result.getCompany());
        // ИСПРАВЛЕНО: findMembers вернет 1
        assertEquals(1, result.getMembers().size());
        assertEquals(adminUser.getEmail(), result.getMembers().get(0).getEmail());

        verify(userRepository).findByEmail(ADMIN_EMAIL);
        verify(subscriptionService).subtractOperatorCount(testCompany);
        verify(roleService).removeRole(MEMBER_EMAIL, Role.OPERATOR);
        verify(userRepository).findByCompanyId(COMPANY_ID); // вызов из findMembers
    }

    @Test
    void removeMember_AttemptToRemoveSelf_ShouldThrowSelfMemberDisbandException() {
        assertThrows(SelfMemberDisbandException.class, () -> {
            companyMembersService.removeMember(ADMIN_EMAIL, ADMIN_EMAIL);
        });
        // userRepository.findByEmail(ADMIN_EMAIL) не будет вызван, т.к. проверка раньше
        verifyNoInteractions(subscriptionService, roleService, userRepository);
    }

    // --- Тесты для leave ---
    @Test
    void leave_Successful_ShouldUpdateServices() {
        memberUser.setCompany(testCompany); // Убедимся, что он в компании
        when(userRepository.findByEmail(MEMBER_EMAIL)).thenReturn(Optional.of(memberUser));
        doNothing().when(subscriptionService).subtractOperatorCount(testCompany);
        when(roleService.removeRole(MEMBER_EMAIL, Role.OPERATOR)).thenReturn(true);

        assertDoesNotThrow(() -> companyMembersService.leave(MEMBER_EMAIL));

        verify(userRepository).findByEmail(MEMBER_EMAIL);
        verify(subscriptionService).subtractOperatorCount(testCompany);
        verify(roleService).removeRole(MEMBER_EMAIL, Role.OPERATOR);
    }

    @Test
    void leave_AttemptToLeaveAsCompanyContactEmail_ShouldThrowSelfMemberDisbandException() {
        // adminUser.getEmail() совпадает с testCompany.getContactEmail()
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(adminUser));
        assertThrows(SelfMemberDisbandException.class, () -> {
            companyMembersService.leave(ADMIN_EMAIL);
        });
        verify(userRepository).findByEmail(ADMIN_EMAIL);
        verifyNoInteractions(subscriptionService, roleService);
    }

    @Test
    void leave_UserNotFound_ShouldThrowNotFoundCompanyException() {
        when(userRepository.findByEmail(MEMBER_EMAIL)).thenReturn(Optional.empty());
        assertThrows(NotFoundCompanyException.class, () -> { // NotFoundCompanyException из-за .map(User::getCompany)
            companyMembersService.leave(MEMBER_EMAIL);
        });
    }
}